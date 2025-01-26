# This program splits the output of curl -i between the informations and the
# body of the response. We can then redirect the body into jq for pretty
# printing.
BEGIN { part = 1 }
/^\r$/ { part = 0; next }
part { print > "/dev/stderr" }
!part { print > "/dev/stdout" }
