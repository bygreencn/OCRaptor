#!/bin/bash
# markdown help_en.md > help_en.html
# http://azaleasays.com/2014/03/04/add-table-of-contents-to-markdown-converted-html-with-python-markdown/
python -m markdown -x toc help_en.md > help_en.html
sed -i '1s/^/<meta charset="utf-8"\/>\n/' help_en.html
sed -i '1s/^/<link rel="stylesheet" href="markdown5.css"\/>\n/' help_en.html
