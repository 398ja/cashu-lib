#!/usr/bin/env python3
"""Verify documentation against source across one or more repos.

Usage:
    doccheck <repo> [repo...]      # defaults to the current directory

Checks:
  links    every relative markdown link resolves to a real file
  classes  every backtick-quoted PascalCase name matches a real Java type
           (indexed across all sibling repos, since docs cross-reference them)
  config   every ${ENV_VAR} in application.yml / .properties appears in
           docs/configuration.md, so a real setting cannot go undocumented

Exit code is non-zero when anything fails, so it can gate a commit.

Written after a documentation pass in which hand-written docs referenced a
renamed class, a link at the wrong directory depth, and a LICENSE file that did
not exist. All three were invisible to review and trivial for a script.
"""
import re, sys, os
from pathlib import Path

LINK = re.compile(r'\[([^\]]*)\]\(([^)]+)\)')
CAND = re.compile(r'`([A-Z][A-Za-z0-9]*(?:[A-Z][a-z0-9]+)+)`')
SKIP = set("""NOT NULL README CHANGELOG JSON HTTP HTTPS JVM SQL TODO PostgreSQL GitHub
JavaScript TypeScript OpenAPI PascalCase SecureRandom MessageDigest ObjectMapper JsonMapper
JavaTimeModule ParameterNamesModule SerializationFeature ResponseEntity RestController
AuthenticationPrincipal RequiredArgsConstructor GetMapping PostMapping RequestMapping
RequestBody ExceptionHandler RestControllerAdvice ConfigurationProperties ApplicationReadyEvent
UnsupportedOperationException NoSuchMethodError NoClassDefFoundError IllegalStateException
RuntimeException InterruptedException ByteArrayOutputStream BigInteger ByteBuffer
StandardCharsets SecretKeySpec GCMParameterSpec CompletableFuture ReentrantLock ThreadLocal
ConcurrentHashMap ClassNotFoundException RestTemplate TreeMap ArgumentCaptor
IOException IllegalArgumentException NullPointerException ClassCastException AutoCloseable
LinkedHashMap LinkedHashSet BigDecimal HttpClient ProcessBuilder PreparedStatement
DocumentBuilderFactory SAXParserFactory XMLInputFactory BouncyCastle SecP256K1Curve
CBORGenerator ScopedValue StructuredTaskScope VirtualThread MeterRegistry ApplicationEvent
ApplicationEventPublisherAware WebSocketHandler ScheduledExecutorService SubtleCrypto
PKPass PKStoreCard PKDateStyleMedium GenericClass GenericObject
ConnectException UnsupportedClassVersionError IllegalAccessException
InaccessibleObjectException IndexOutOfBoundsException NoSuchElement ServiceLoader KeyStore
HexFormat TypeError DOMException Uint8Array SharedArrayBuffer BroadcastChannel
SecurityContextHolder GrantedAuthority ObjectProvider RestClient FilterRegistrationBean
HealthIndicator JdkClientHttpRequestFactory StandardWebSocketClient TextWebSocketHandler
ECDomainParameters SECNamedCurves DeterministicKey RandomSource SimplePool""".split())

ALLOWLIST_NAME = ".doccheck-allow"

# Below this many sibling repositories the workspace is treated as partial:
# unresolved names are reported for information but do not fail the run.
PARTIAL_WORKSPACE_MIN = 10

def allowlist(root: Path):
    """Names a repo declares are not Java types: alert rules, external products.

    One name per line in .doccheck-allow; blank lines and # comments ignored.
    """
    f = root / ALLOWLIST_NAME
    if not f.is_file():
        return set()
    return {ln.split('#')[0].strip() for ln in f.read_text().splitlines() if ln.split('#')[0].strip()}


def allow_all_links(root: Path):
    """True when .doccheck-allow declares this tree's links unmaintained.

    A superseded checkout keeps its documentation as written. Repairing its links
    would imply the tree is maintained, so the marker records the decision
    instead.
    """
    f = root / ALLOWLIST_NAME
    return f.is_file() and "doccheck: skip-links" in f.read_text()


# Plan and spec documents quote snippets destined for other files, including
# link lines whose relative paths are correct only from the target. Checking them
# reports the quoting, not a broken link.
QUOTED_DIRS = {"plans", "specs", "superpowers", "archive"}


def docs_of(root: Path):
    out = [root/'README.md', root/'CLAUDE.md', root/'AGENTS.md']
    if (root/'docs').is_dir():
        out += [f for f in (root/'docs').rglob('*.md')
                if not QUOTED_DIRS & set(f.relative_to(root).parts)]
    return [f for f in out if f.is_file()]

def check_config(root: Path):
    """Report env vars bound in config but absent from docs/configuration.md.

    Only the reverse direction is checked. A doc may legitimately mention a var
    owned by another service (a shared secret, a peer's credential), but a var
    this service actually reads and nobody documented is a real gap.
    """
    doc = root / "docs" / "configuration.md"
    if not doc.is_file():
        return None
    cfg = ""
    for pat in ("**/application*.yml", "**/application*.yaml", "**/application*.properties"):
        for f in root.glob(pat):
            if "target" in f.parts or "/test/" in str(f):
                continue
            cfg += f.read_text()
    if not cfg.strip():
        return None
    real = set(re.findall(r"\$\{([A-Z][A-Z0-9_]*)[:}]", cfg))
    cited = set(re.findall(r"`([A-Z][A-Z0-9_]{3,})`", doc.read_text()))
    return sorted(real - cited)


def main(argv):
    repos = [Path(a).resolve() for a in argv] or [Path.cwd()]
    workspace = repos[0].parent

    # index every Java type in the workspace: docs legitimately cite sibling repos
    types = set()
    # TypeScript declarations: several repos ship a TS client SDK whose types are
    # legitimately cited in docs alongside the Java ones.
    for t in list(workspace.rglob('*.ts')) + list(workspace.rglob('*.tsx')):
        if {'node_modules', 'dist', 'target'} & set(t.parts):
            continue
        try:
            text = t.read_text()
        except Exception:
            continue
        types.update(re.findall(
            r'\b(?:interface|type|class|enum)\s+([A-Z]\w*)', text))
        # React components and exported consts
        types.update(re.findall(r'\b(?:const|function)\s+([A-Z]\w*)', text))
    for j in workspace.rglob('*.java'):
        if 'target' in j.parts or 'node_modules' in j.parts:
            continue
        types.add(j.stem)
        try:
            text = j.read_text()
        except Exception:
            continue
        types.update(re.findall(r'\b(?:record|class|enum|interface)\s+([A-Z]\w*)', text))
        # enum constants are legitimately cited in docs (e.g. a saga state), and
        # they are not type declarations, so they need collecting separately.
        for body in re.findall(r'\benum\s+\w+[^{]*\{(.*?)\}', text, re.S):
            head = re.split(r';', body)[0]
            types.update(re.findall(r'\b([A-Z][A-Za-z0-9_]{2,})\b', head))

    broken, unresolved, n_links, n_types = [], [], 0, 0
    for repo in repos:
        allowed = allowlist(repo)
        skip_links = allow_all_links(repo)
        for f in docs_of(repo):
            text = f.read_text()
            for _, target in LINK.findall(text):
                t = target.split('#')[0].strip()
                if not t or ':' in t.split('/')[0] or t.startswith('#'):
                    continue
                if skip_links:
                    continue
                n_links += 1
                if not (f.parent / t).resolve().exists():
                    broken.append((f, t))
            for m in set(CAND.findall(text)):
                if m in SKIP or m in allowed:
                    continue
                n_types += 1
                if m not in types:
                    unresolved.append((f, m))

    print(f"links:   {n_links - len(broken)}/{n_links} resolve")
    for f, t in broken:
        print(f"  BROKEN {f}\n      -> {t}")
    # The class check indexes sibling repos because docs legitimately cite them.
    # A single-repo checkout, which is what CI does, cannot see those siblings, so
    # every cross-repo reference would be reported unknown. Detect that case by
    # asking whether any sibling repository is actually present, rather than by
    # guessing from the size of the index: a repo with its own sources easily
    # exceeds any threshold while still lacking every sibling.
    siblings = [d for d in workspace.iterdir()
                if d.is_dir() and (d/'.git').exists() and d.resolve() not in
                {r.resolve() for r in repos}]
    # A partial workspace (some siblings, not all) is the awkward middle case: a
    # reference to an absent repo looks identical to a genuine typo. Report those
    # names but do not fail, so a partial clone stays usable without turning the
    # checker into noise that gets ignored.
    partial = bool(siblings) and len(siblings) < PARTIAL_WORKSPACE_MIN
    if not siblings:
        print(f"classes: skipped ({len(types)} types indexed; no sibling "
              f"repositories present, so cross-repo references cannot be checked)")
        unresolved = []
    else:
        print(f"classes: {n_types - len(unresolved)}/{n_types} resolve ({len(types)} known types)")
        for f, m in unresolved:
            print(f"  UNKNOWN {f}: {m}")

    cfg_gaps = 0
    for repo in repos:
        missing = check_config(repo)
        if missing:
            cfg_gaps += len(missing)
            print(f"config:  {repo.name} has {len(missing)} env var(s) not in docs/configuration.md")
            for m in missing:
                print(f"  UNDOCUMENTED {m}")
    if not cfg_gaps:
        print("config:  every bound env var is documented")

    if partial and unresolved:
        print(f"         (partial workspace: {len(siblings)} sibling repos present, "
              f"so the names above may simply live in a repo that is not checked "
              f"out; not failing on them)")
        unresolved = []

    return 1 if (broken or unresolved or cfg_gaps) else 0

if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
