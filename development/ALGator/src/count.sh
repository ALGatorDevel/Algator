#!/bin/bash

# Use first argument or default to current directory
DIR="${1:-.}"

# Check if directory exists
if [ ! -d "$DIR" ]; then
    echo "Error: '$DIR' is not a valid directory"
    exit 1
fi

count_files=0
total_lines=0

# Traverse .java files
while IFS= read -r -d '' file; do
    ((count_files++))
    
    lines=$(wc -l < "$file")
    ((total_lines += lines))

done < <(find "$DIR" -type f -name "*.java" -print0)

# Output
echo "Directory: $DIR"
echo "Number of .java files: $count_files"
echo "Total number of lines: $total_lines"
