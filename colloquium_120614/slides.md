% Linguistic Data Validation and Observation
% John P. M<sup>c</sup>Crae
% 12th June 2014

# Problem

<div class="leftcol">
<br/>
<br/>
  <p class="icon"><img src="icon_47466i.png" width="30%"/></p>
  <p class="icon">Lots of resources</p>
</div>
<div class="rightcol">
<br/>
<br/>
  <p class="icon"><img src="icon_44875i.png" width="30%"/></p>
  <p class="icon">Frequently contain flaws</p>
</div>

# Case Study: WordNets

Open Multilingual WordNets:

<div class="leftcol">
* Albanian
* Arabic
* Chinese (Taiwan)
* Danish
* Finnish
* French
* Hebrew
* Italian
* Japanese
</div>
<div class="rightcol">
* Spanish
* Catalan
* Basque
* Galician
* Malay
* Norwegian
* Polish
* Portuguese
* Thai

</div>

# Case Study: WordNets

Formats:

<div style="text-align:center;">
<img src="wn_formatsi.png" style="max-height:60%;"/>
</div>


# Case Study: WordNets

Data quality:

<div style="text-align:center;">
<img src="wn_errorsi.png" style="max-height:60%;"/>
</div>


# Solution

<div class="leftcol">
<br/>
<br/>
  <p class="icon"><img src="icon_40065i.png" width="30%"/></p>
  <p class="icon">Data certification</p>
</div>
<div class="rightcol">
<br/>
<br/>
  <p class="icon"><img src="icon_3683i.png" width="30%"/></p>
  <p class="icon">Metadata</p>
</div>

# Data certification

Requirements:

* Open data
* Single URL
* Standard format

# Zrtifi

<!--<br/>
<br/>
<h1>[Click for Demo](index.html)</h1>-->
<iframe src="index.html" width="100%" height="77%"></iframe>

# Backend

<div style="width:100%;text-align:center;">
<img src="Zrtifi Backend Flowi.png"/>
</div>

# Rules

* We don't store data
* New steps can be added via GitHub
* Linear, stream-based validation

# Data-sniffer

Inversion of control service

    class Sniffer {
       boolean isInFormat(String fileName, 
                          byte[] firstKilobyte);
       String chain();
    }

# Validators

* Single shell command
* Return JSON-LD

<pre>
{
    "@context": "http://...",
    "result": "success",
    "void:triples": 123456,
    "next" : "rdfunit"
}
</pre>

# Observatory

* Check dataset every 24 hours
* HTTP handshake (200 Accept)
* Check `Last-Modified`

# Metadata

<div class="leftcol" style="height:80%;width:100%;">
  <div style="padding: 20%;">
     Datahub.io
  </div>
  <div style="padding: 20%;">
     Metashare
  </div>
</div>
<div class="rightcol">
  <div style="padding: 20%;">
    CLARIN
  </div>
  <div style="padding: 20%;">
    LRE Map
  </div>
</div>

# Statistics

<div style="text-align:center;">
<img src="repo_sizesi.png" style="max-height:77%;"/>
</div>

# LingHub

* Define common metadata (DCAT + VoID)
* Bring everything 'under one roof'
* Find duplicates

# Zrtifi-LingHub
   
* Incorporate output from Zrtifi
* Triple counts
* Validation


# Better LLOD Cloud 

<div style="text-align:center;">
<img src="llod-cloud.currenti.png" style="max-height:77%;"/>
</div>

# Thanks

<br/>
<br/>
<h3><a href="http://jmccrae.github.io/zrtifi/colloquium_120614/slides.html">Slides on GitHub</a></h3>

<h3><img src="by-nc.png" width="100px;"/></h3>

<div style="font-size:30%;">
Heavy Load designed by Juan Pablo Bravo from the Noun Project<br/>
Floppy Disk designed by Julien Deveaux from the Noun Project<br/>
Warranty designed by Eugen Belyakoff from the Noun Project<br/>
Network designed by Ben Rex Furneaux from the Noun Project<br/>
</div>
