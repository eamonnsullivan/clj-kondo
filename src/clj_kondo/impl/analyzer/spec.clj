(ns clj-kondo.impl.analyzer.spec
  {:no-doc true}
  (:require
     [clj-kondo.impl.analyzer.common :as common]
     [clj-kondo.impl.findings :as findings]
     [clj-kondo.impl.linters.keys :as keys]
     [clj-kondo.impl.namespace :as namespace]
     [clj-kondo.impl.utils :as utils]))

(defn analyze-fdef [{:keys [:analyze-children :ns] :as ctx} expr]
  (let [[sym-expr & body] (next (:children expr))
        ns-nm (-> ns :name)]
    (keys/lint-map-keys ctx {:children body} {:known-key? #{:args :ret :fn}})
    (let [sym (:value sym-expr)]
      (if-not (and sym (symbol? sym))
        (findings/reg-finding! ctx
                               (utils/node->line (:filename ctx)
                                                 sym-expr
                                                 :error
                                                 :syntax
                                                 "expected symbol"))
        (let [{resolved-ns :ns}
              (namespace/resolve-name ctx ns-nm
                                      sym)]
          (if resolved-ns
            (namespace/reg-used-namespace! ctx ns-nm resolved-ns)
            (findings/reg-finding! ctx
                                   (utils/node->line (:filename ctx)
                                                     sym-expr
                                                     :error
                                                     :unresolved-symbol
                                                     (str "Unresolved symbol: " sym)))))))
    (analyze-children ctx body)))

(defn analyze-def [ctx expr fq-def]
  (let [[name-expr & body] (next (:children expr))
        reg-val (if (:k name-expr)
                  (assoc name-expr :def fq-def)
                  name-expr)]
    (common/analyze-children ctx (cons reg-val body))))

(defn analyze-lazy-combinators [ctx expr]
  (let [vars (next (:children expr))
        decl-node (utils/token-node 'clojure.core/declare)
        new-expr (assoc expr :children (cons decl-node vars))]
    (common/analyze-expression** ctx new-expr)))

;;;; Scratch
(require '[clj-kondo.impl.parser])

(comment
  (:lines (first (:children (clj-kondo.impl.parser/parse-string "\"foo\"")))))
