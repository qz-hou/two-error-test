;; gorilla-repl.fileformat = 1

;; @@
(ns clojush.instructions.genome  
  (:use [clojush pushstate globals args random]
        clojush.instructions.common
        clojush.pushgp.genetic-operators))

(define-registered genome_pop (with-meta (popper :genome) {:stack-types [:genome]}))
(define-registered genome_dup (with-meta (duper :genome) {:stack-types [:genome]}))
(define-registered genome_swap (with-meta (swapper :genome) {:stack-types [:genome]}))
(define-registered genome_rot (with-meta (rotter :genome) {:stack-types [:genome]}))
(define-registered genome_flush (with-meta (flusher :genome) {:stack-types [:genome]}))
(define-registered genome_eq (with-meta (eqer :genome) {:stack-types [:genome :boolean]}))
(define-registered genome_stackdepth (with-meta (stackdepther :genome) {:stack-types [:genome :integer]}))
(define-registered genome_yank (with-meta (yanker :genome) {:stack-types [:genome :integer]}))
(define-registered genome_yankdup (with-meta (yankduper :genome) {:stack-types [:genome :integer]}))
(define-registered genome_shove (with-meta (shover :genome) {:stack-types [:genome :integer]}))
(define-registered genome_empty (with-meta (emptyer :genome) {:stack-types [:genome :boolean]}))

(define-registered
  genome_gene_dup
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state)))
             (< (count (first (:genome state)))
                (/ @global-max-points 4)))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (into (subvec genome 0 (inc index))
                              (subvec genome index))
                        :genome)))
      state)))

(define-registered
  genome_gene_randomize
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (assoc genome 
                          index
                          (random-plush-instruction-map 
                            @global-atom-generators
                            {:epigenetic-markers @global-epigenetic-markers
                             :close-parens-probabilities @global-close-parens-probabilities
                             :silent-instruction-probability @global-silent-instruction-probability}))
                        :genome)))
      state)))

(define-registered
  genome_gene_replace
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (rest (:integer state))))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))
            atom-gen (mod (stack-ref :integer 1 state) (count @global-atom-generators))]
        (->> (pop-item :integer state)
             (pop-item :integer)
             (pop-item :genome)
             (push-item (assoc genome
                          index
                          (random-plush-instruction-map 
                                    [(nth @global-atom-generators atom-gen)]
                                    {:epigenetic-markers @global-epigenetic-markers
                                     :close-parens-probabilities @global-close-parens-probabilities
                                     :silent-instruction-probability @global-silent-instruction-probability}))
                        :genome)))
      state)))

(define-registered
  genome_gene_delete
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (into (subvec genome 0 index)
                              (subvec genome (inc index)))
                        :genome)))
      state)))

(define-registered
  genome_rotate
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            distance (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (into (subvec genome distance)
                              (subvec genome 0 distance))
                        :genome)))
      state)))

(define-registered
  genome_gene_copy
  ^{:stack-types [:genome :integer]}
  ;; copies from the second genome to the first
  ;; index is into source -- if destination is too short it will be added to end
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (rest (:genome state))))
             (not (empty? (stack-ref :genome 1 state))))
      (let [source (stack-ref :genome 1 state)
            destination (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count source))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (assoc (vec destination)
                          (min index (count destination))
                          (nth source index))
                        :genome)))
      state)))

(define-registered
  genome_gene_copy_range
  ^{:stack-types [:genome :integer]}
  ;; copies from the second genome to the first
  ;; indices are into source -- if destination is too short they will be added to end
  (fn [state]
    (if (and (not (empty? (rest (:integer state))))
             (not (empty? (rest (:genome state))))
             (not (empty? (stack-ref :genome 1 state))))
      (let [source (stack-ref :genome 1 state)
            destination (stack-ref :genome 0 state)
            indices [(mod (stack-ref :integer 0 state) (count source))
                     (mod (stack-ref :integer 1 state) (count source))]
            low-index (apply min indices)
            high-index (apply max indices)]
        (->> (pop-item :integer state)
          (pop-item :integer)
          (pop-item :genome)
          (push-item (loop [i low-index
                            result destination]
                       (if (> i high-index)
                         result
                         (recur (inc i)
                                (assoc result
                                  (min i (count destination))
                                  (nth source i)))))
                     :genome)))
      state)))

(define-registered
  genome_toggle_silent
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (assoc genome
                          index
                          (let [g (nth genome index)]
                            (assoc g :silent (not (:silent g)))))
                        :genome)))
      state)))

(define-registered
  genome_silence
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (assoc genome
                          index
                          (let [g (nth genome index)]
                            (assoc g :silent true)))
                        :genome)))
      state)))

(define-registered
  genome_unsilence
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (assoc genome
                          index
                          (let [g (nth genome index)]
                            (assoc g :silent false)))
                        :genome)))
      state)))

(define-registered
  genome_close_inc
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (assoc genome
                          index 
                          (let [g (nth genome index)]
                            (assoc g :close (inc (:close g)))))
                        :genome)))
      state)))

(define-registered
  genome_close_dec
  ^{:stack-types [:genome :integer]}
  (fn [state]
    (if (and (not (empty? (:integer state)))
             (not (empty? (:genome state)))
             (not (empty? (stack-ref :genome 0 state))))
      (let [genome (stack-ref :genome 0 state)
            index (mod (stack-ref :integer 0 state) (count genome))]
        (->> (pop-item :integer state)
             (pop-item :genome)
             (push-item (assoc genome
                          index
                          (let [g (nth genome index)]
                            (assoc g :close (max 0 (dec (:close g))))))
                        :genome)))
      state)))

(define-registered
  genome_new
  ^{:stack-types [:genome]}
  (fn [state]
    (push-item [] :genome state)))

(define-registered
  genome_parent1
  ^{:stack-types [:genome]}
  (fn [state]
    (push-item (vec (:parent1-genome state)) :genome state)))

(define-registered
  genome_parent2
  ^{:stack-types [:genome]}
  (fn [state]
    (push-item (vec (:parent2-genome state)) :genome state)))

(define-registered
  autoconstructive_integer_rand 
  ;; pushes a constant integer, but is replaced with integer_rand during 
  ;; nondetermistic autoconstruction
  ^{:stack-types [:genome :integer]} (fn [state] (push-item 0 :integer state)))

(define-registered
  autoconstructive_boolean_rand 
  ;; pushes false, but is replaced with boolean_rand during 
  ;; nondetermistic autoconstruction
  ^{:stack-types [:genome :boolean]} (fn [state] (push-item false :boolean state)))

(define-registered
  genome_uniform_instruction_mutation
  ^{:stack-types [:genome :float]}
  (fn [state]
    (if (and (not (empty? (:float state)))
             (not (empty? (:genome state))))
      (let [rate (mod (first (:float state)) 1.0)
            genome (first (:genome state))]
        (->> (pop-item :float state)
             (pop-item :genome)
             (push-item (vec (:genome (uniform-instruction-mutation
                                        {:genome genome}
                                        (merge @push-argmap {:uniform-mutation-rate rate}))))
                        :genome)))
      state)))

(define-registered
  genome_uniform_integer_mutation
  ^{:stack-types [:genome :integer :float]}
  (fn [state]
    (if (and (not (empty? (rest (:float state))))
             (not (empty? (:genome state))))
      (let [rate (mod (first (:float state)) 1.0)
            stdev (+ 1 (#(if (pos? %) % (- %)) (second (:float state))))
            genome (first (:genome state))]
        (->> (pop-item :float state)
             (pop-item :float)
             (pop-item :genome)
             (push-item (vec (:genome (uniform-integer-mutation
                                        {:genome genome}
                                        (merge @push-argmap
                                               {:uniform-mutation-constant-tweak-rate rate
                                                :uniform-mutation-int-gaussian-standard-deviation stdev}))))
                        :genome)))
      state)))
;; @@
