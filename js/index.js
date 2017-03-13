/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Prism from 'prism'
import PrismJson from 'prism-json'
import PrismMarkdown from 'prism-markdown'
import { PrismPreviewerBase } from 'prism-previewer-base'
import { PrismPreviewerColor } from 'prism-previewer-color'
import { TabbedContent } from './TabbedContent.js'
import Util from './util.js'
/* global $, $$ */

document.addEventListener('DOMContentLoaded', function (event) {
  // load tabs
  let content = document.querySelector('.StyleguideContent')
  let tabbedContent = new TabbedContent(content, {})
  tabbedContent.init()
  let searchObject = Util.locationSearchToObject(window.location.search)

  $$('.StyleguideNavigation a').forEach(function (element) {
    if (element.pathname === searchObject['file']) {
      element.setAttribute('data-active', '')
      tabbedContent.initTabs(element)
    }

    element.addEventListener('click', function (event) {
      window.history.replaceState({}, this.getAttribute('title'), '?file=' + this.pathname)
      // loop through and remove the active nav attribute
      $$('.StyleguideNavigation a[data-active]').forEach(function (element) {
        element.removeAttribute('data-active')
      })
      this.setAttribute('data-active', '')
      tabbedContent.createTabs(this)
      if (event.target.classList.contains('StyleguideGroups-examples-externalLink')) {
        window.open(this.href)
      }
    })
  })
}, false)
