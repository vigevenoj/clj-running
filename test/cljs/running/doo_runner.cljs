(ns running.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [running.core-test]))

(doo-tests 'running.core-test)

