import tarfile
import os.path
import os
import sys
from uuid import uuid4

ZRTIFI_ONTOLOGY = "http://www.zrtifi.org/ontology#"

if __name__ == "__main__":
    step_id = str(uuid4())
    dir os.path.abspath(os.path.join(sys.argv[1], os.pardir))
    file = sys.argv[1]
    if os.path.exists(file):
        if file.endswith(".tar"):
            tfile = tarfile.open(file)
            print("<> <%sstep> <#tar_step_%s> ." % (ZRTIFI_ONTOLOGY, step_id))
            print("<#tar_step_%s> <%sprocess> \"tar\" ." % (step_id, ZRTIFI_ONTOLOGY))
            for filename in tarfile:
                if ".." in filename:
                    print("<#tar_step_%s> <%signored> \"%s\" . " % (step_id, ZRTIFI_ONTOLOGY, filename))
                else:
                    targ_dir = os.path.abspath(os.path.join(dir + os.sep + filename,os.pardir))
                    if not os.path.exists(targ_dir):
                        os.makedirs(targ_dir)
                    tarfile.extract(filename, dir + os.sep + filename)
                    file_id = str(uuid4())
                    print("<#file_%s> <http://www.zrtifi.org/internal#next> <sniff> ." % file_id)
                    print("<#file_%s> <http://www.zrtifi.org/internal#nextTarget> <file:%s> ." % (file_id, dir + os.sep + filename))
            print("<#tar_step_%s> <%sstatus> <%ssuccess> ." % (step_id, ZRTIFI_ONTOLOGY, ZRTIFI_ONTOLOGY))
        else:
            print("<#tar_step_%s> <%serror> \"file does not end in .tar\"@en ." % (step_id, ZRTIFI_ONTOLOGY))
            print("<#tar_step_%s> <%sstatus> <%sfailed> ." % (step_id, ZRTIFI_ONTOLOGY, ZRTIFI_ONTOLOGY))
    else:
        print("<#tar_step_%s> <%serror> \"file does not exists\"@en ." % (step_id, ZRTIFI_ONTOLOGY))
        print("<#tar_step_%s> <%sstatus> <%sfailed> ." % (step_id, ZRTIFI_ONTOLOGY, ZRTIFI_ONTOLOGY))
        
