(ns slamhound.test-reconstruct
  "Testing some things going on here."
  (:use [clojure.test])
  (:require [slam.hound.asplode :as asplode]
            [slam.hound.regrow :as regrow]
            [slam.hound.stitch :as stitch]))

(def sample-ns-form '(ns slamhound.sample
                       "Testing some things going on here."
                       (:refer-clojure :exclude [compile test])
                       (:use [clojure.test :only [deftest is]]
                             [slam.hound.stitch :only [ns-from-map]])
                       (:require [clojure.java.io :as io]
                                 [clojure.set :as set])
                       (:import (java.io File ByteArrayInputStream)
                                (java.util UUID))))

(def sample-ns-map
  {:name 'slamhound.sample
   :meta {:doc "Testing some things going on here."}
   :use '([clojure.test :only [deftest is]] [slam.hound.stitch
                                             :only [ns-from-map]])
   :require '([clojure.java.io :as io] [clojure.set :as set])
   :import '((java.io File ByteArrayInputStream) (java.util UUID))
   :refer-clojure '(:exclude [compile test])})

(def sample-body
  '((set/union #{:a} #{:b})
    (UUID/randomUUID)
    (io/copy (ByteArrayInputStream. (.getBytes "remotely human"))
             (doto (File. "/tmp/remotely-human") .deleteOnExit))
    (deftest test-ns-to-map
      (is (= (ns-from-map {:ns 'slam.hound}))))))

;;; representation transformation

(deftest test-ns-to-map
  (is (= sample-ns-map (asplode/ns-to-map sample-ns-form))))

(deftest test-ns-from-map
  (is (= sample-ns-form (stitch/ns-from-map sample-ns-map))))

(deftest test-roundtrip
  (is (= sample-ns-form (stitch/ns-from-map (asplode/ns-to-map sample-ns-form)))))

;;; regrow

(deftest test-grow-import
  (is (= (assoc sample-ns-map
           ;; this is pre-collapse
           :import '(java.io.File java.io.ByteArrayInputStream java.util.UUID))
         (regrow/regrow [(dissoc sample-ns-map :import) sample-body]))))

(deftest test-grow-require
  (is (= sample-ns-map (regrow/regrow [(dissoc sample-ns-map :require) sample-body]))))

#_(deftest test-grow-use
  (is (= sample-ns-map (regrow/regrow [(dissoc sample-ns-map :use) sample-body]))))
