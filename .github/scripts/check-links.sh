#!/bin/sh

function check_link {
	# we remove any in-page anchor
	polished=$(echo $link | cut -d'#' -f 1)
	if [ -z "$polished" ]; then
		# in-page link
		return 0
	fi

	if [[ $polished == https://* ]]; then
		curl -o /dev/null -Ifs "$polished"
		if [[ $? -ne 0 ]]; then
			echo "- broken web link: $polished"
			return 1
		fi
	else
		if [ ! -f "$(dirname $1)/$polished" ]; then
			echo "- broken file ref: $polished"
			return 1
		fi
	fi
	return 0
}

readmes=$(find $1 -type f -regex "[^_]*\.md")
code=0
for file in ${readmes[@]}; do
	echo Checking $file
	hreflinks=$(grep -oP '(?<=href=").*?(?=")' $file)
	varlinks=$(grep -oP '(?<=\]:).*?(?=$)' $file)
	inlinelinks=$(grep -oP '(?<=\]\().*?(?=\))' $file)
	
	for link in ${hreflinks}; do
		check_link $file $link
		let "code=code+$?"
	done
	
	for link in ${varlinks}; do
		check_link $file $link
		let "code=code+$?"
	done
	
	for link in ${inlinelinks}; do
		check_link $file $link
		let "code=code+$?"
	done
done

exit $code
