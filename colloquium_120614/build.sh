#!/bin/bash

cp header slides.html
pandoc -t dzslides slides.md >> slides.html
cat footer >>slides.html
