Standalone native notice inventory
==================================

These notices are copied from the exact local source checkouts used by the
standalone build. They are packaged as APK assets under notices/native/;
their presence is not a claim that this directory is a complete
corresponding-source bundle.

PRoot
-----
Source: https://github.com/termux/proot.git
Tag: v5.1.107.92
Commit: 7266fb3e8516535682f5a9c8f3a7e70f6506eddb
Pinned archive URL: https://github.com/termux/proot/archive/v5.1.107.92.zip
Pinned archive SHA-256: 29385d1ddb619a9c4449ab512bfd55032034b22f724ddf98fc95ff300ea32135
Notice: proot/COPYING
Build recipe: native/build-proot.sh
Patch: native/patches/proot-android-includes.patch
Patch SHA-256: 3f464fbc1c55952cfa4aedd5e153885f1cbfef293da0043c54b5fb95bc3e2901

talloc
------
Source archive: https://www.samba.org/ftp/talloc/talloc-2.4.2.tar.gz
Source archive SHA-256: 85ecf9e465e20f98f9950a52e9a411e14320bc555fa257d87697b7e7a9b1d8a6
Notice evidence: talloc/talloc-source-header.txt is the pinned source header;
it states that the talloc library uses the GNU Lesser General Public License,
version 3 or any later version. talloc/LGPL-3.txt is the exact LGPL-3 text
present in the pinned Ubuntu guest common-license set. talloc/NEWS records the
source history for the license change in lib/replace. This is notice evidence,
not the complete talloc source tree.
Build recipe: native/build-proot.sh

libandroid-shmem
----------------
Source: https://github.com/termux/libandroid-shmem/archive/refs/tags/v0.7.tar.gz
Tag: v0.7
Archive SHA-256: 1e5ff8459bc0a8c229dd8a94b27d119987e09ef3414331c2b5ebfff20b98e867
Notice: libandroid-shmem/LICENSE
Build recipe: native/build-proot.sh
Patch: native/patches/libandroid-shmem-runtime-tmp.patch
Patch SHA-256: c276c03e38cb020d0c49b1be9ef3e5af3adb74de22da7f5f29f09ebbf73b0cbe

Termux:X11 / Lorie
------------------
Source: https://github.com/termux/termux-x11.git
Commit: bd1cfadd74f98b548662a48b6e1d564aa434c86e
Root license SHA-256: 3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986
Build recipe: prepare.sh and standalone/build.sh
Checked-in patch: patches/x11-gl-header.patch
Checked-in patch SHA-256: d4185458795338375324eadd0a199dc993822ed2c55ffe9369494e552c4f4e1d
Upstream vendored patch: lorie/src/main/cpp/patches/libepoxy.patch
Upstream vendored patch SHA-256: 7ba07fbb866977622b1f072b805b29dcaacdf208d6eb6d8cc761ea4f61b4c59d

The termux-x11/lorie subtree below this directory contains the license files
found in the pinned submodules (including bzip2, libepoxy, libfontenc,
libtirpc, libx11, libxau, libxcvt, libxdmcp, libxfont, libxkbfile,
libxshmfence, libxtrans, pixman, xkbcomp, xorgproto, xserver, and Lorie's
shm component). The corresponding source trees and all generated build
products are not included in this notice asset.

The exact submodule commits used for that notice snapshot are recorded by the
pinned checkout's gitlink state:

6a8690fc8d26c815e798c588f796eabe9d684cf0  bzip2
c84bc9459357a40e46e2fec0408d04fbdde2c973  libepoxy
780d1f6f192a331de7b298a96e23ebc44d2a884b  libfontenc
5ca4ca92f629d9d83e83544b9239abaaacf0a527  libtirpc
6c75545a1deb51f5903992c52af6bc35cc9bc103  libx11
a9c65683e68b3a4349afee5d7673b393fb924d2e  libxau
dd8631c61465cc0de5e476c7a98e56528d62b163  libxcvt
1192d3bc407348ff316bd3bffc791b3ac73f591b  libxdmcp
88b9df882c0ea91d7bb468aa4792ef1b6f13aa9d  libxfont
42e5dedd7fd3c7c73f3870a8751893c03c1afc69  libxkbfile
89f064748554e11832a3ec783945e1f4c7fe846e  libxshmfence
cf05ba4a10c90da2c63805a5375e983b174e28b0  libxtrans
9cc163c9da0fb4da430641715313d95a6ec466d9  pixman
2c7789785981ad1fce3858c726615b49293f7de0  xkbcomp
fcb7e9a1a0b593a44740d83b0babddd331fea830  xorgproto
65d790bd208ec380b196eb98f144abb0b32e334d  xserver
