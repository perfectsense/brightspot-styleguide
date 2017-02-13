/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import {TabbedContent} from '../TabbedContent.js'

document.addEventListener('DOMContentLoaded', function (event) {
  function searchToObject (search) {
    let searchParams = search.substring(1).split('&')
    let searchParam
    let paramObj = {}

    for (let key in searchParams) {
      if (searchParams[key] === null) continue

      searchParam = searchParams[key].split('=')
      paramObj[ decodeURIComponent(searchParam[0]) ] = decodeURIComponent(searchParam[1])
    }
    return paramObj
  }

  // load tabs
  let content = document.querySelector('.StyleguideContent')
  let tabbedContent = new TabbedContent(content, {})
  tabbedContent.init()
  let searchObject = searchToObject(window.location.search)

  $$('.StyleguideNavigation a').forEach(function (element) {
    if (element.pathname === searchObject['file']) {
      element.setAttribute('data-active', '')
      tabbedContent.initTabs(element)
    }

    element.addEventListener('click', function (event) {
      history.replaceState({}, this.getAttribute('title'), '?file=' + this.pathname)
      // loop through and remove the active nav attribute
      $$('.StyleguideNavigation a[data-active]').forEach(function (element) {
    	   element.removeAttribute('data-active')
      })
      this.setAttribute('data-active', '')
      tabbedContent.createTabs(this)
      if (event.target.classList.contains('fa-external-link')) {
        window.open(this.href)
      }
    })
  })
}, false)
