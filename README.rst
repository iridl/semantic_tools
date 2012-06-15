IRI DL Semantic Tools
*********************

* Prerequisites:

   * To run: Java 1.6+.

   * To build: Java 1.6+, GNU make 3.81+, and Unix friendly environment. (Tested on Linux and Darwin platforms.)

* To obtain the sources (including underlying submodules)::

   git clone --recursive git@bitbucket.org:iridl/semantic_tools.git

* To build::

   make

* To install, for example, to ``~/semantic_tools``::

   make PREFIX=~/semantic_tools install

* To create a tarball for distribution::

   make tarball

* To run semantic tools::

   export PATH=~/semantic_tools/bin:$PATH
   rdfcache
   xmlfromsesame

