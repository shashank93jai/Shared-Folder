for i in $(echo $1 | tr "/" "\n"); 
do 
	if [ "$(file $j$i | cut -d " " -f 2)" == "directory" ] ; then
		j="$j$i/";
	elif [ "$(file $j$i | cut -d " " -f 2)" == "ERROR:" ] ; then	
		
		x=$(echo $i |grep "\.")
		if [ "$x" == "" ]; then
			`mkdir ./$j$i`;
#			echo ./$j/$i;
		else	
			`touch ./$j$i`;
#			echo ./$j/$i;
		fi
		j="$j$i/";
	fi		
done 
