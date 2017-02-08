/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import {TabbedContent} from '../TabbedContent.js'

document.addEventListener('DOMContentLoaded', function (event) {
  let content = document.querySelector('.StyleguideContent')
  let tabbedContent = new TabbedContent(content, {})
  tabbedContent.init()

  $$('.StyleguideNavigation a').forEach(function (element) {
    element.addEventListener('click', function (event) {
      tabbedContent.createTabs(this)
      // set first tab to init on click
      $('.StyleguideTabs li').setAttribute('data-active', '')
    })
  })
}, false)
