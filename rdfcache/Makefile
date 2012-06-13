PREFIX ?= .

INSTALL = install
ANT = ant

.PHONY: build clean distclean install

build:
	$(ANT) all

clean distclean:
	$(ANT) clean

install: build
	$(INSTALL) -d $(PREFIX)/lib
	$(INSTALL) -d $(PREFIX)/bin
	$(INSTALL) -m 0644 build/jar/generatentriples.jar $(PREFIX)/lib
	$(INSTALL) rdfcache $(PREFIX)/bin

