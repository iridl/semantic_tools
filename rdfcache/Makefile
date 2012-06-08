PREFIX ?= .

INSTALL = install
ANT = ant

.PHONY: build clean distclean install

build:
	$(ANT) build

clean distclean:
	$(RM) -r build

install: build
	$(INSTALL) -d $(PREFIX)/lib
	$(INSTALL) -m 0644 build/jar/generatentriples.jar $(PREFIX)/lib

