package com.github.jmccrae.zrtifi

class Template(contents : String) {
  // TODO: Optimize
  def substitute(subs : (String,String)*) = {
    var c = contents
    for((subFor,subTo) <- subs) {
      c = c.replaceAll("\\$\\{?%s\\}?" format subFor, subTo)
    }
    c.replaceAll("\\$\\$","\\$")
  }
}
