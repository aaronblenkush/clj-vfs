# clj-vfs

A Clojure library wrapping the [Apache Commons VFS API](https://commons.apache.org/proper/commons-vfs/).

## Usage

Add the dependency to your project:

```clojure
[clj-vfs "0.1.0"]
```

Examples:

```clojure
(require [clj-vfs.core :as vfs])

;; create a reusable connection
(def con (vfs/connect "sftp://hostname" "username" "password"))

;; directory listing
(vfs/ls con "")
;; => ("/file1" "/file2")

;; file details
(vfs/details con "/file1")
;; => {:size 123456, :last-modified-time 999999999, :attributes {}}

;; upload file
(vfs/put con "/path/to/local-file" "/remote-path")
;; => nil

;; remove file
(vfs/rm con "/remote-path")
;; => true
```

## License

Copyright © 2017 Aaron Blenkush

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.

## Acknowledgements

Thanks to [gerritjvv](https://github.com/gerritjvv) for providing the [snippet]
(https://gist.github.com/gerritjvv/5807270) I used to create the library.
