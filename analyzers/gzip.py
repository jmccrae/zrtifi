import gzip
import sys
from subprocess import call
from os.path import exists

if __name__ == "__main__":
    file = sys.argv[1]

    if exists(file):
        if file.endswith(".gz"):
            call(["gunzip", file])
            print("<> <http://zrtifi.org/success> \"gzip\" .")
            print("<> <http://zrtifi.org/next> <http://zritif.org/sniff> .")
            print("<> <http://zrtifi.org/nextTarget> \"%s\" ." % (file[:-3]))
        else:
            print("<> <http://zrtifi.org/error> \"file does not end in .gz\"@en .")
    else:
        print("<> <http://zrtifi.org/error> \"file does not exist\"@en . ")
