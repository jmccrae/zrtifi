from subprocess import Popen, PIPE
from cStringIO import StringIO
import sys
from uuid import uuid4

step_id = str(uuid4())
ZRTIFI_ONTOLOGY = "http://www.zrtifi.org/ontology#"

def format_err(lines):
    for line in lines:
        yield line.strip()

if __name__ == "__main__":
    p = Popen(["xmllint","--noouti","--loaddtd ",sys.argv[1]], stdout=PIPE, stderr=PIPE)
    p.wait()
    print("<> <%sstep> <#xml_step_%s> ." % (ZRTIFI_ONTOLOGY, step_id))
    print("<#xml_step_%s> <%sprocess> \"xml\" ." % (step_id, ZRTIFI_ONTOLOGY))
    if p.returncode == 0:
        print("<#xml_step_%s> <%sstatus> <%ssuccess> ." % (step_id, ZRTIFI_ONTOLOGY, ZRTIFI_ONTOLOGY))
    elif p.returncode >= 1:
        print("<#xml_step_%s> <%sstatus> <%serror> ." % (step_id, ZRTIFI_ONTOLOGY, ZRTIFI_ONTOLOGY))
        errmsg = "\\n".join(format_err(p.stderr.readlines()))
        print("<#xml_step_%s> <%serror> \"%s\" ." % (step_id, ZRTIFI_ONTOLOGY, errmsg))
        if p.returncode == 1:
           print("<#xml_step_%s> <%serror> \"Unclassified\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 2:
           print("<#xml_step_%s> <%serror> \"Error in DTD\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 3:
           print("<#xml_step_%s> <%serror> \"Validation error\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 4:
           print("<#xml_step_%s> <%serror> \"Validation error\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 5:
           print("<#xml_step_%s> <%serror> \"Error in schema compilation\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 6:
           print("<#xml_step_%s> <%serror> \"Error writing output\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 7:
           print("<#xml_step_%s> <%serror> \"Error in pattern (generated when --pattern option is used)\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 8:
           print("<#xml_step_%s> <%serror> \"Error in Reader registration (generated when --chkregister option is used)\" ." % (step_id, ZRTIFI_ONTOLOGY))
        if p.returncode == 9:
           print("<#xml_step_%s> <%serror> \"Out of memory error\" ." % (step_id, ZRTIFI_ONTOLOGY))
    
