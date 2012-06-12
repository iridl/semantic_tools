PREFIX ?= .

BUILD_DIR = ___build

INSTALL = install
TAR = tar
MV = mv
MICONF_GVI = $(miconf)/scripts/git-generate-version-info ingrid
MICONF_PLAT = $(miconf)/scripts/miconf-platform

ifdef TARGET
   BTARGET = $(TARGET)
else
   BTARGET = build
endif

allcomponents = olfs rdfcache xmldocs

.PHONY: build cleanlocal clean distclean tarball install version $(allcomponents)

build: $(allcomponents)

$(allcomponents):
	$(MAKE) --directory=$@ PREFIX=$(abspath $(BUILD_DIR)) $(MARGS) $(BTARGET)

cleanlocal:
	$(RM) -r $(BUILD_DIR)
	
clean: cleanlocal
	$(MAKE) PREFIX=$(abspath $(BUILD_DIR)) $(MARGS) TARGET=clean

distclean: cleanlocal
	$(MAKE) PREFIX=$(abspath $(BUILD_DIR)) $(MARGS) TARGET=distclean

install:
	$(MAKE) PREFIX=$(abspath $(BUILD_DIR)) $(MARGS) TARGET=install
	$(INSTALL) -d $(PREFIX)
	$(INSTALL) -d $(BUILD_DIR)/lib
	$(INSTALL) -m 0644 libs/* $(BUILD_DIR)/lib
	$(TAR) cf - -C $(BUILD_DIR) . | $(TAR) xf - -C $(PREFIX)

