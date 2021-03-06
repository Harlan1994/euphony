(ns euphony.test-system
  (:require [clojure.test :as t]
            [euphony.protocols.conn :as pc]
            [euphony.system :as sys]
            [euphony.utils.io :as io]))

(def CONF {:conn {:uri "datomic:mem://test"
                           :reset-on-start true
                           :no-side-effect true}})

(def PARSE-DATOMS "test/data/parse-datoms.edn")
(def IMPORT-DATOMS "test/data/import-datoms.edn")
(def CLUSTER-DATOMS "test/data/cluster-datoms.edn")
(def RESULTS-DATOMS "test/data/results-datoms.edn")

                                        ; DYNAMIC VARS

;; main components
(def ^:dynamic *sys*)

;; example datasets
(def ^:dynamic *results*)

;; database + datoms
(def ^:dynamic *conn-initial*)
(def ^:dynamic *conn-after-parse*)
(def ^:dynamic *conn-after-import*)
(def ^:dynamic *conn-after-cluster*)
(def ^:dynamic *conn-after-results*)

                                        ; FIXTURES FUNCTIONS

(defn with-sys [f]
  (sys/with-system [system CONF]
    (binding [*sys* system]
      (f))))

(defn- conn-initial [f]
  (let [conn (:conn *sys*)]
    (binding [*conn-initial* conn]
      (f))))

(def with-conn-initial (t/compose-fixtures with-sys conn-initial))

(defn- after-import [f]
  (binding [*conn-after-import* (pc/transact *conn-initial* (io/read-edn! IMPORT-DATOMS))]
    (f)))

(def with-conn-after-import (t/compose-fixtures with-conn-initial after-import))

(defn- after-parse [f]
  (binding [*conn-after-parse* (pc/transact *conn-after-import* (io/read-edn! PARSE-DATOMS))]
    (f)))

(def with-conn-after-parse (t/compose-fixtures with-conn-after-import after-parse))

(defn- after-cluster [f]
  (binding [*conn-after-cluster* (pc/transact *conn-after-parse* (io/read-edn! CLUSTER-DATOMS))]
    (f)))

(def with-conn-after-cluster (t/compose-fixtures with-conn-after-parse after-cluster))

(defn with-results [f]
  (let [results-datoms (io/read-edn! RESULTS-DATOMS)
        to-tuple (juxt :result/antivirus :result/label)
        results (->> results-datoms (filter :result/label) (map to-tuple))]
    (binding [*results* results]
      (f))))

(defn- after-results [f]
  (let [results-datoms (io/read-edn! RESULTS-DATOMS)]
    (binding [*conn-after-results* (pc/transact *conn-initial* results-datoms)]
      (f))))

(def with-conn-after-results (t/compose-fixtures with-conn-initial after-results))
