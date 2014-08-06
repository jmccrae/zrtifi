from subprocess import Popen, PIPE
import sys
from uuid import uuid4
import re

step_id = str(uuid4())
ZRTIFI_ONTOLOGY = "http://www.zrtifi.org/ontology#"

if __name__ == "__main__":
    p = Popen(["rapper","-i","guess","-c",sys.argv[1]], stdout=PIPE, stderr=PIPE)
    p.wait()
    print("<> <%sstep> <#rdf_step_%s> ." % (ZRTIFI_ONTOLOGY, step_id))
    print("<#rdf_step_%s> <%sprocess> \"rdf\" ." % (step_id, ZRTIFI_ONTOLOGY))
    if p.returncode == 0:
        print("<#rdf_step_%s> <%sstatus> <%ssuccess> ." % (step_id, ZRTIFI_ONTOLOGY, ZRTIFI_ONTOLOGY))
    else:
        print("<#rdf_step_%s> <%sstatus> <%serror> ." % (step_id, ZRTIFI_ONTOLOGY, ZRTIFI_ONTOLOGY))

    for line in p.stderr.readlines():
        m = re.match("rapper: Parsing returned (\\d+) triples", line)
        if m:
            triples = int(m.group(1))
            print("<#rdf_step_%s> <http://rdfs.org/ns/void#triples> \"%d\"^^<http://www.w3.org/2001/XMLSchema#integer> ." % (step_id, triples))
        elif line.startswith("rapper: Parsing URI"):
            pass
        elif line.startswith("rapper: Error -"):
            print("<#rdf_step_%s> <%serror> \"%s\" ." % (step_id, ZRTIFI_ONTOLOGY, line[len("rapper: Error -"):].strip()))
