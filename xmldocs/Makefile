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
	$(INSTALL) -m 0644 build/jar/iriwms.jar $(PREFIX)/lib
	$(INSTALL) -m 0644 build/dist/xmldocs.war $(PREFIX)/lib
	$(INSTALL) xmlfromsesame $(PREFIX)/bin
	$(INSTALL) saxon_transform $(PREFIX)/bin


