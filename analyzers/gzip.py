import gzip
import sys
from subprocess import Popen, PIPE
from os.path import exists
from uuid import uuid4

step_id = str(uuid4())
def format_err(lines):
    for line in lines:
        yield line.strip()

ZRTIFI_ONTOLOGY = "http://www.zrtifi.org/ontology#"

if __name__ == "__main__":
    file = sys.argv[1]

    if exists(file):
        if file.endswith(".gz"):
            p = Popen(["gunzip", file], stderr=PIPE)
            p.wait()
            print("<> <%sstep> <#gzip_step_%s> ." % (ZRTIFI_ONTOLOGY, step_id))
            print("<#gzip_step_%s> <%sprocess> \"gzip\" ." % (step_id, ZRTIFI_ONTOLOGY))
            if p.returncode == 0:
                print("<#gzip_step_%s> <%sstatus> <http://www.zrtifi.org/ontology#success> ." % (step_id, ZRTIFI_ONTOLOGY))
                print("<#file_%s> <http://www.zrtifi.org/internal#next> <sniff> ." % step_id)
                print("<#file_%s> <http://www.zrtifi.org/internal#nextTarget> <%s> ." % (step_id, file[:-3]))
                print("<> <%scontains> <#file_%s> ." % (ZRTIFI_ONTOLOGY, step_id))
                print("<#file_%s> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://www.w3.org/ns/dcat#Distribution> ." % (step_id))
            else:
                print("<#gzip_step_%s> <%sstatus> <http://www.zrtifi.org/ontology#error> ." % (step_id, ZRTIFI_ONTOLOGY))
                print("<#gzip_step_%s> <%sstatus> \"%s\" ." % (step_id, ZRTIFI_ONTOLOGY,
                    "\\n".join(format_err(p.stderr.readlines()))))
                
        else:
            print("<#gzip_step_%s> <%serror> \"file does not end in .gz\"@en ." % (step_id, ZRTIFI_ONTOLOGY))
            print("<#gzip_step_%s> <%sstatus> <http://www.zrtifi.org/ontology#failed> ." % (step_id, ZRTIFI_ONTOLOGY))
    else:
        print("<#gzip_step_%s> <%sstatus> <http://www.zrtifi.org/ontology#failed> ." % (step_id, ZRTIFI_ONTOLOGY))
        print("<#gzip_step_%s> <%serror> \"file does not exist\"@en . " % (step_id, ZRTIFI_ONTOLOGY))
