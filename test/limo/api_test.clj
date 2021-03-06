(ns limo.api-test
  (:require [clojure.test :refer :all]
            [limo.api :refer :all]
            [limo.driver :refer :all]
            [limo.test :refer :all]
            [limo.java :refer :all]))

(deftest opening-chrome-headless
  (with-fresh-browser create-chrome-headless
    (to "https://httpbin.org")
    (click "a[href='/html']")
    (is (text= "h1" "Herman Melville - Moby-Dick"))))

(deftest opening-a-browser
  (with-fresh-browser create-chrome
    (to "https://httpbin.org")
    (click "a[href='/html']")
    (is (text= "h1" "Herman Melville - Moby-Dick"))))

(deftest multiple-windows
  (with-fresh-browser create-chrome
    (to "https://httpbin.org")
    (is (text= "h1" "httpbin(1): HTTP Request & Response Service"))
    (execute-script *driver* "window.open('https://now.httpbin.org/');")
    (is (= 2 (count (all-windows))))
    (switch-to-window (last (all-windows)))
    (is (contains-text? "body" "/when/:human-timestamp"))
    (switch-to-window (first (all-windows)))
    (is (text= "h1" "httpbin(1): HTTP Request & Response Service"))))

(deftest without-implicit-driver
  (set-driver! nil) ;; just to be sure
  (let [driver (create-chrome)]
    (to driver "https://httpbin.org")
    (is (text= driver "h1" "httpbin(1): HTTP Request & Response Service"))
    (execute-script driver "window.open('https://now.httpbin.org/');")
    (is (= 2 (count (all-windows driver))))
    (switch-to-window driver (last (all-windows driver)))
    (is (contains-text? driver "body" "/when/:human-timestamp"))
    (switch-to-window driver (first (all-windows driver)))
    (is (text= driver "h1" "httpbin(1): HTTP Request & Response Service"))
    (click driver "a[href='/html']")
    (is (text= driver "h1" "Herman Melville - Moby-Dick"))))

(deftest network
  (with-fresh-browser (partial create-chrome (set-logging-capability (->capabilities :chrome)
                                                                     {:browser     :all
                                                                      :performance :all}))
    (to "https://httpbin.org")
    (execute-script *driver* "var r = new XMLHttpRequest(); r.open(\"GET\", \"/get\", null); r.send();")
    (let [logs (atom [])]
      (read-performance-logs-until-test-pass! [logs]
        (is (first (filter (comp #{"Network.requestWillBeSent"} :method :message :message) @logs)))))))

(deftest test-various-by-locators
  (with-fresh-browser create-chrome
    (to "https://httpbin.org")
    (click-when-visible {:partial-link-text "anything"})
    (is (= (current-url) "https://httpbin.org/anything"))

    (to "https://httpbin.org")
    ;; Note: click on link with label 'now.httpbin.org'
    (click-when-visible {:xpath "//*[@id=\"manpage\"]/div/ul[1]/li/a/code"})
    (is (= (current-url) "https://now.httpbin.org/"))

    (to "https://httpbin.org")
    ;; Note: click on the non secure link using :css-selector locator
    (click-when-visible {:css-selector "#manpage > div > p:nth-child(2) > a:nth-child(1)"})
    (is (= (current-url) "http://httpbin.org/"))

    (to "https://httpbin.org")
    ;; Note: click on the secure link using :css-selector locator
    (click-when-visible {:css-selector "#manpage > div > p:nth-child(2) > a:nth-child(2)"})
    (is (= (current-url) "https://httpbin.org/"))

    (to "https://httpbin.org")
    ;; Note: click on the secure link using :css locator (the other selector for css)
    (click-when-visible {:css "#manpage > div > p:nth-child(2) > a:nth-child(2)"})
    (is (= (current-url) "https://httpbin.org/"))))
