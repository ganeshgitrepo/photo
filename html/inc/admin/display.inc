<center>
<img src="%ITOP%/%IMAGE%"><br>
</center>
<form method="POST" action="/cgi-bin/dustin/photo/admin/edittext.cgi">
<input type="hidden" name="oid" value="%OID%">

Size:  %SIZE%<br>

Category:
<select name="cat">
%CATS%
</select><br>

Taken:
<input name="taken" value="%TAKEN%"><br>

Keywords:
<input name="keywords" value="%KEYWORDS%"><br>

Info:
<textarea cols="60" rows="5" name="info" wrap="hard">%INFO%</textarea><br>

<input type="submit" value="Save Info">
<input type="reset" value="Restore to Original">

</form>