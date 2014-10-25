(ns trail-journals-scrape.core
  (:require [clj-http.client :as client]
            [net.cgrand.enlive-html :as e])
  (:gen-class))
  
  (defn- retrieve [suffix]
    (let [url        (str "http://www.trailjournals.com/" suffix)
          user-agent "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.172"]
      (:body (client/get url {:headers {"User-Agent" user-agent}}))))
 
  (defn- get-content [link] 
    (let [content   (e/html-snippet (retrieve link))
          text      (e/text (first (e/select content [:blockquote])))
          title     (e/text (first (e/select content [:i])))
          next-link (get-in (first (e/select content [:td  [:a (e/pred #(= (e/text %) "Next"))]])) [:attrs :href]  )]
      {:next-link next-link :text text :title title}))

  (defn- blogs-starting-from [start-entry]
    (letfn [(blogs 
	      ([]  (blogs (get-content start-entry)))
	      ([n] (cons n (lazy-seq (blogs (get-content (:next-link n)))))))] blogs))

  (defn- convert-to-html [post]
    (str "<h1>" (:title post) "</h1>" (:text post)))

  (defn- wrap-as-doc [html-fragment]
    (str "<html><head><meta charset=\"UTF-8\"></head><body>" html-fragment "</body></html>"))
  
  (defn- write-blog [[blog-name start-post]]
    (->>
      ((blogs-starting-from start-post))
      (take-while :next-link)
      (map convert-to-html)
      (apply str)
      (wrap-as-doc)
      (spit (str (name blog-name) ".html"))))
   
 (defn -main [& args]
   (doseq [blog {:at "entry.cfm?id=126097", :pct "entry.cfm?id=309359", :cdt "entry.cfm?id=420096"}]
     (write-blog blog))) 

