IRI DL Semantic Tools
*********************

* To obtain the sources (including underlying submodules)::

   git clone git@bitbucket.org:iridl/semantic_tools.git
   cd semantic_tools
   git submodule update --recursive --init

* To build, install, and run semantic tools, for example::

   make PREFIX=/usr/local/semantic_tools install
   export PATH=/usr/local/semantic_tools/bin:$PATH
   rdfcache
   xmlfromsesame

* To create tarball::

   make tarball

* To clean up::

   make distclean


