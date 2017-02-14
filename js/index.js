/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Util from './util.js'
import {TabbedContent} from './TabbedContent.js'

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
