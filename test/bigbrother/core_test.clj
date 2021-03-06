(ns bigbrother.core-test
  (:require
   [bigbrother.core :refer :all]

   [clojure.test :refer :all]
   [clojure.test.check.generators :as gen]
   [clojure.test.check.properties :as prop]
   [clojure.test.check.clojure-test :refer [defspec]]))

(deftest timer-test
  (do
    (reset-all-atoms!)
    ;; first loop
    (let [session (telescreen-on)]
      (Thread/sleep session 30)
      (log-time session :x1)
      (Thread/sleep session 30)
      (log-time session :x2)
      (Thread/sleep session 30)
      (log-time session :end)
      (telescreen-off session)
      ;; second loop
      (log-time session :start)
      (Thread/sleep 30)
      (log-time session :x2)
      (Thread/sleep 30)
      (log-time session :end)
      (telescreen-off session)
      (end-session! session))
    (let [result (resume-map 1000)
          result2 (resume-map 2000)
          ]
      (is (contains? result :nb))
      (is (contains? result :x1))
      (is (contains? result :x2))
      (is (= (:x1 result) (:x1 result2)))
      (is (= (:x2 result) (:x2 result2)))
      (is (>= (:total result) (:x2 result)))
      (is (= (/ (:nb result) 2) (:nb result2))))))

(deftest check-metrics
  (do
    (reset-all-atoms!)
    ;; first loop
    (log-metric :foo 1)
    (log-metric :bar 3)
    (timer-loop-finished)
    (log-metric :foo 2)
    (log-metric :bar 2)
    (timer-loop-finished)
    (log-metric :foo 3)
    (log-metric :bar 1)
    (timer-loop-finished)
    (let [result (resume-map 1000)
          result2 (resume-map 2000)
          ]
      (is (contains? result :foo))
      (is (contains? result :bar))
      (is (contains? result :nb))
      (is (= 2.0 (:foo result)))
      (is (= 2.0 (:bar result)))
      (is (= 3.0 (:nb result)))
      (is (= (/ (:nb result) 2) (:nb result2))))))

(deftest check-mmetrics
  (do
    (reset-all-atoms!)
    (log-mmetric :foo 80)
    (log-mmetric :bar 4)
    (timer-loop-finished)
    (log-mmetric :foo 50)
    (log-mmetric :bar 5)
    (timer-loop-finished)
    (log-mmetric :foo 60)
    (log-mmetric :bar 6)
    (timer-loop-finished)
    (let [result (resume-map 1000)
          result2 (resume-map 2000)
          ]
      (is (contains? result :foo))
      (is (contains? result :bar))
      (is (contains? result :nb))
      (is (= 80 (:foo result)))
      (is (= (:foo result) (:foo result2)))
      (is (= 6  (:bar result)))
      (is (= (:bar result)  (:bar result2)))
      (is (= 3.0 (:nb result))))))

(deftest check-counters
  (do
    (reset-all-atoms!)
    ;; first loop
    (log-counter :foo)
    (log-counter :foo)
    (log-counter :bar 3)
    (timer-loop-finished)
    (log-counter :foo)
    (log-counter :bar 2)
    (timer-loop-finished)
    (log-counter :foo)
    (log-counter :bar 1)
    (timer-loop-finished)
    (let [result (resume-map 1000)]
      (is (contains? result :foo))
      (is (= 4.0 (:foo result)))
      (is (= 6.0 (:bar result)))
      (is (= 3.0 (:nb result))))))

(defspec check-all-keys-exists
  30
  (prop/for-all
   [actions (gen/such-that #(> (count %) 1)
                           (gen/vector gen/keyword))]
   (do
     (reset-all-atoms!)
     (let [session (telescreen-on)]

       (doall (map (fn [a] (do (log-time session a) (Thread/sleep 10)))
                   actions))
       (timer-loop-finished session)
       (end-session! session)))
   (let [result (resume-map 1000)]
     (every? #(contains? result %) (rest actions)))))

(defspec check-total-is-greater-than-each-input
  30
  (prop/for-all
   [actions (gen/such-that #(> (count %) 1)
                           (gen/vector gen/keyword))]
   (do
     (reset-all-atoms!)
     (let [session (telescreen-on)]
       (doall (map (fn [a] (do (log-time session a) (Thread/sleep 10)))
                   actions))
       (timer-loop-finished session)
       (end-session! session)))
   (let [result (resume-map 1000)]
     (every? #(>= (:total result) (get result %)) (rest actions)))))
