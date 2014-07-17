import gzip
import sys
from subprocess import call
from os.path import exists

if __name__ == "__main__":
    file = sys.argv[1]

    if exists(file):
        if file.endswith(".gz"):
            call(["gunzip", file])
            print("<> <http://www.zrtifi.org/ontology#step> <#gzip_step> .")
            print("<#gzip_step> <http://www.zrtifi.org/ontology#process> \"gzip\" .")
            print("<#gzip_step> <http://www.zrtifi.org/ontology#status> <http://www.zrtifi.org/ontology#success> .")
            print("<> <http://www.zrtifi.org/internal#next> <sniff> .")
            print("<> <http://www.zrtifi.org/internal#nextTarget> <%s> ." % (file[:-3]))
        else:
            print("<#gzip_step> <http://zrtifi.org/ontology#error> \"file does not end in .gz\"@en .")
            print("<#gzip_step> <http://www.zrtifi.org/ontology#status> <http://www.zrtifi.org/ontology#failed> .")
    else:
        print("<#gzip_step> <http://www.zrtifi.org/ontology#status> <http://www.zrtifi.org/ontology#failed> .")
        print("<#gzip_step> <http://zrtifi.org/ontology#error> \"file does not exist\"@en . ")
