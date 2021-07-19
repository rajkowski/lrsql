(ns lrsql.ops.query.document
  (:require [clojure.spec.alpha :as s]
            [com.yetanalytics.lrs.protocol :as lrsp]
            [lrsql.interface.protocol :as ip]
            [lrsql.spec.common :as c :refer [transaction?]]
            [lrsql.spec.document :as ds]
            [lrsql.util :as u]
            [lrsql.ops.util :refer [throw-invalid-table-ex]]))

(s/fdef query-document
  :args (s/cat :tx transaction? :input ds/document-input-spec)
  :ret ::lrsp/get-document-ret)

(defn query-document
  "Query a single document from the DB. Returns a map where the value of
   `:document` is either a map with the contents as a byte array, or `nil`
   if not found."
  [interface tx {:keys [table] :as input}]
  (if-some [res (case table
                  :state-document
                  (ip/-query-state-document interface tx input)
                  :agent-profile-document
                  (ip/-query-agent-profile-document interface tx input)
                  :activity-profile-document
                  (ip/-query-activity-profile-document interface tx input)
                  ;; Else
                  (throw-invalid-table-ex "query-document" input))]
    (let [{contents     :contents
           content-type :content_type
           content-len  :content_length
           state-id     :state_id
           profile-id   :profile_id
           updated      :last_modified}
          res]
      {:document
       {:contents       contents
        :content-length content-len
        :content-type   content-type
        :id             (or state-id profile-id)
        :updated        (u/time->str updated)}})
    ;; Not found
    {:document nil}))

(s/fdef query-document-ids
  :args (s/cat :tx transaction? :input ds/document-ids-query-spec)
  :ret ::lrsp/get-document-ids-ret)

;; TODO: The LRS should also return last modified info.
;; However, this is not supported in Milt's LRS spec.
(defn query-document-ids
  "Query multiple document IDs from the DB. Returns a map containing the
   vector of IDs."
  [interface tx {:keys [table] :as input}]
  (let [ids (case table
              :state-document
              (->> input
                   (ip/-query-state-document-ids interface tx)
                   (map :state_id))
              :agent-profile-document
              (->> input
                   (ip/-query-agent-profile-document-ids interface tx)
                   (map :profile_id))
              :activity-profile-document
              (->> input
                   (ip/-query-activity-profile-document-ids interface tx)
                   (map :profile_id))
              ;; Else
              (throw-invalid-table-ex "query-document-ids" input))]
    {:document-ids (vec ids)}))
