(TeX-add-style-hook
 "jsonSyntax"
 (lambda ()
   (TeX-add-to-alist 'LaTeX-provided-package-options
                     '(("paralist" "defblank")))
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "url")
   (add-to-list 'LaTeX-verbatim-macros-with-braces-local "path")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "url")
   (add-to-list 'LaTeX-verbatim-macros-with-delims-local "path")
   (TeX-run-style-hooks
    "latex2e"
    "article"
    "art10"
    "xspace"
    "amsfonts"
    "amsmath"
    "amsthm"
    "amssymb"
    "mathtools"
    "csquotes"
    "paralist"
    "booktabs"
    "graphicx"
    "color"
    "url"
    "xcolor"
    "enumitem"
    "fancyvrb")
   (TeX-add-symbols
    '("exFont" 1)))
 :latex)

