/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import {DeviceViewport} from './DeviceViewport.js'
import Util from './util.js'
import {TabbedContent} from './TabbedContent.js'
/* global $, $$ */

document.addEventListener('DOMContentLoaded', function (event) {
  // load tabs
  // initialize viewport controls
  let viewportControls = document.querySelector('.StyleguideViewport-controls')
  let deviceViewport = new DeviceViewport(viewportControls, {})
  deviceViewport.init()

  // initialize tabs
  let content = document.querySelector('.StyleguideContent')
  let tabbedContent = new TabbedContent(content, {})
  tabbedContent.init()
  let searchObject = Util.locationSearchToObject(window.location.search)
  // loop through styleguide navigation to find matching file path and init tabs based on whats active
  $$('.StyleguideNavigation a').forEach(function (element) {
    if (element.pathname === searchObject['file']) {
      element.setAttribute('data-active', '')
      tabbedContent.initTabs(element)
    }
    // bind all navigation items to a click event and create tabs for data-sources (json, markdown, ect)
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
