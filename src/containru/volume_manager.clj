(ns containru.volume-manager)

(defprotocol VolumeManager
  (create [this]) ;; return id
  (prepare [this id]) ;; get volume ready for use return path to volume
  (sleep [this id]) ;; hook for when a container gets slept
  (delete [this id])) ;; deletes the volume

(deftype FSVolumeManager [root]
  VolumeManager
  (create [this]
    (let [id (str (java.util.UUID/randomUUID))
          path (str root "/" id)]
      (.mkdir (java.io.File. path))
      id))
  (prepare [this id]
    (str root "/" id))
  (sleep [this id] nil)
  (delete [this id]
    (.delete (java.io.File. (str root "/" id)))))
(defn new-fs [root] (FSVolumeManager. root))
