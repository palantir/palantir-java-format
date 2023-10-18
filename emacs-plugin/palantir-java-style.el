;;; palantir-java-style.el --- Indentation style matching palantir-java-format -*- lexical-binding: t -*-

;; Author: Will Dey
;; Maintainer: Will Dey
;; Version: 1.0.0
;; Package-Requires: ()
;; Homepage: https://github.com/palantir/palantir-java-format

;; This file is not part of GNU Emacs

;;; Code:

;;;###autoload
(defun palantir-java-style-lineup-anchor (langelem)
  (vector (c-langelem-col langelem :preserve-point)))

(defvar c-syntactic-context)
;;;###autoload
(defun palantir-java-style-lineup-single-arg (_langelem)
  (save-excursion
    (forward-line)
    (when (assq 'statement-cont (c-guess-basic-syntax))
      '++)))

;;;###autoload
(defun palantir-java-style-lineup-cascaded-calls (langelem)
  (save-excursion
    (back-to-indentation)
    (when (looking-at (rx ?.))
      '++)))

;;;###autoload
(defconst palantir-java-style
  '("java"
    (indent-tabs-mode . nil)
    (c-basic-offset . 4)
    (c-offsets-alist . ((statement-cont . ++)
			(arglist-cont . (first palantir-java-style-lineup-cascaded-calls 0))
			(arglist-intro . (add palantir-java-style-lineup-anchor ++ palantir-java-style-lineup-single-arg))
			(arglist-cont-nonempty . ++)
			(template-args-cont . 16)))))

;;;###autoload
(c-add-style "palantir" palantir-java-style)

(provide 'palantir-java-style)

;;; palantir-java-style.el ends here
