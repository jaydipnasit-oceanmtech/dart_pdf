 # Copyright (C) 2017, David PHAM-VAN <dev.nfet.net@gmail.com>
 #
 # Licensed under the Apache License, Version 2.0 (the "License");
 # you may not use this file except in compliance with the License.
 # You may obtain a copy of the License at
 #
 #     http://www.apache.org/licenses/LICENSE-2.0
 #
 # Unless required by applicable law or agreed to in writing, software
 # distributed under the License is distributed on an "AS IS" BASIS,
 # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 # See the License for the specific language governing permissions and
 # limitations under the License.

 DART_SRC=$(shell find . -name '*.dart')
 CLNG_SRC=$(shell find printing/ios -name '*.java' -o -name '*.m' -o -name '*.h') $(shell find printing/android -name '*.java' -o -name '*.m' -o -name '*.h')
 FONTS=pdf/open-sans.ttf pdf/roboto.ttf

all: $(FONTS) format

pdf/open-sans.ttf:
	curl -L "https://github.com/google/fonts/raw/master/apache/opensans/OpenSans-Regular.ttf" > $@

pdf/roboto.ttf:
	curl -L "https://github.com/google/fonts/raw/master/apache/robotomono/RobotoMono-Regular.ttf" > $@

format: format-dart format-clang

format-dart: $(DART_SRC)
	dartfmt -w --fix $^

format-clang: $(CLNG_SRC)
	clang-format -style=Chromium -i $^

pdf/.dart_tool:
	cd pdf ; pub get

test: pdf/.dart_tool $(FONTS)
	cd pdf; for EXAMPLE in $(shell cd pdf; find example -name '*.dart'); do dart $$EXAMPLE; done
	cd pdf; for TEST in $(shell cd pdf; find test -name '*.dart'); do dart $$TEST; done
	# cd printing; flutter test

clean:
	git clean -fdx

publish-pdf: format clean
	cd pdf; pub publish -f

publish-printing: format clean
	cd printing; pub publish -f

.PHONY: test format format-dart format-clang clean publish-pdf publish-printing