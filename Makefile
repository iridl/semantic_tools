PLAT ?= $(shell $(MICONF_PLAT))
ARCH ?= $(shell arch)
PREFIX ?= /usr/local/$(VER)

BUILD_DIR = ___build

INSTALL = install
TAR = tar
MV = mv
MICONF_GVI = miconf/scripts/git-generate-version-info semantic_tools
MICONF_PLAT = miconf/scripts/miconf-platform

VER = $(shell $(MICONF_GVI) raw)
TARBALL = $(VER)

MARGS = PLAT=$(PLAT) ARCH=$(ARCH)

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
	$(INSTALL) -m 0644 libs/*.jar $(BUILD_DIR)/lib
	$(TAR) cf - -C $(BUILD_DIR) . | $(TAR) xf - -C $(PREFIX)

tarball:
	$(MAKE) PREFIX=$(abspath $(TARBALL)) $(MARGS) install
	$(TAR) cvfz $(VER).tgz $(TARBALL)
	$(RM) -r $(TARBALL)

