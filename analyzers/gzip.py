import gzip
import sys
from subprocess import Popen, PIPE
from os.path import exists
from uuid import uuid4

step_id = str(uuid4())
def format_err(lines):
    for line in lines:
        yield line.strip()

if __name__ == "__main__":
    file = sys.argv[1]

    if exists(file):
        if file.endswith(".gz"):
            p = Popen(["gunzip", file], stderr=PIPE)
            p.wait()
            print("<> <http://www.zrtifi.org/ontology#step> <#gzip_step_%s> ." % step_id)
            print("<#gzip_step_%s> <http://www.zrtifi.org/ontology#process> \"gzip\" ." % step_id)
            if p.returncode == 0:
                print("<#gzip_step_%s> <http://www.zrtifi.org/ontology#status> <http://www.zrtifi.org/ontology#success> ." % step_id)
                print("<#file_%s> <http://www.zrtifi.org/internal#next> <sniff> ." % step_id)
                print("<#file_%s> <http://www.zrtifi.org/internal#nextTarget> <%s> ." % (step_id, file[:-3]))
            else:
                print("<#gzip_step_%s> <http://www.zrtifi.org/ontology#status> <http://www.zrtifi.org/ontology#error> ." % step_id)
                print("<#gzip_step_%s> <http://www.zrtifi.org/ontology#status> \"%s\" ." % (step_id,
                    "\\n".join(format_err(p.stderr.readlines()))))
                
        else:
            print("<#gzip_step_%s> <http://zrtifi.org/ontology#error> \"file does not end in .gz\"@en ." % step_id)
            print("<#gzip_step_%s> <http://www.zrtifi.org/ontology#status> <http://www.zrtifi.org/ontology#failed> ." % step_id)
    else:
        print("<#gzip_step_%s> <http://www.zrtifi.org/ontology#status> <http://www.zrtifi.org/ontology#failed> ." % step_id)
        print("<#gzip_step_%s> <http://zrtifi.org/ontology#error> \"file does not exist\"@en . " % step_id)
