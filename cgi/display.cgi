#!/usr/local/bin/perl
#
# Copyright (c) 1997  Dustin Sallings
#
# $Id: display.cgi,v 1.2 1997/11/03 09:31:45 dustin Exp $

use Postgres;

$dbh=db_connect("photo");
$Itop="/~dustin/photo/album";

$query ="select a.oid,a.fn,a.keywords,a.descr,\n";
$query.="    a.size,a.taken,a.ts,b.name,a.cat,b.id\n";
$query.="    from album a, cat b ";
$query.="    where a.cat=b.id and a.oid=$ARGV[0];";

if( !($s=$dbh->execute($query)) )
{
    print "Content-type: text/html\n\n";
    print "ERROR!!!  $Postgres::error<br>\n$query<br>\n";
    exit(0);
}

($oid, $image, $keywords, $info, $size, $taken, $timestamp, $cat)
    =$s->fetchrow();

print <<EOF;
Content-type: text/html

<html><head><title>Info for $oid</title></head>
<body bgcolor="#fFfFfF">

<center>
<img src="$Itop/$image">
</center>
<p>

Category: $cat
<p>

Size: $size bytes
<p>

Taken: $taken
<p>

Added: $timestamp
<p>

Keywords:<br>
$keywords
<p>

Info:<br>
$info

<hr>
</body></html>
EOF
