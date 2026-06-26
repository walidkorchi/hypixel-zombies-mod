import csv, re, sys, os, pathlib
mcp = "tools/mcp"
mapping = {}
for fn in ("methods.csv","fields.csv"):
    with open(os.path.join(mcp,fn), newline='') as f:
        for row in csv.DictReader(f):
            mapping[row['searge']] = row['name']
tok = re.compile(r'(?:func|field)_\d+_\w+')
src_root = sys.argv[1]
unmapped = set()
count = 0
for p in pathlib.Path(src_root).rglob("*.java"):
    text = p.read_text()
    def sub(m):
        global count
        s = m.group(0)
        if s in mapping:
            count += 1
            return mapping[s]
        unmapped.add(s)
        return s
    new = tok.sub(sub, text)
    if new != text:
        p.write_text(new)
print(f"replaced {count} references")
if unmapped:
    print("UNMAPPED:", sorted(unmapped))
