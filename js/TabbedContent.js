/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Prism from 'prism'
import PrismJson from 'prism-json'
import PrismMarkdown from 'prism-markdown'
/* global $, $$ */

export class TabbedContent {
  get selectors () {
    return this.settings.selectors
  }

  get dataType () {
    return this._dataType
  }

  set dataType (data) {
    this._dataType = data
  }

  get iframeStyle () {
    return this._iframeStyle
  }

  set iframeStyle (style) {
    this._iframeStyle = style
  }

  constructor (ctx, options = {}) {
    this.ctx = ctx
    this.settings = Object.assign({}, {
      selectors: {
        tabList: 'StyleguideTabs',
        content: 'StyleguideContent',
        iframeContent: 'StyleguideExample'
      },
      prismMap: {'json': 'json', 'documentation': 'markdown'}
    }, options)
  }

  init () {
    let self = this
    $.create('ul', {className: this.selectors.tabList})._.start($(`.${this.selectors.content}`))
    // event listener for iframed content; uses Prism plugin to highlight elements
    $(`.${this.selectors.iframeContent}`).addEventListener('load', function (event) {
      if (self.settings.prismMap[self.dataType] !== undefined) {
        let prismElement = this.contentWindow.document.querySelector('pre')
        prismElement.className = `language-${self.settings.prismMap[self.dataType]}`
        Prism.highlightElement(prismElement)
        let cssAppend = $.clone($('link[href="/_styleguide/index.css"]'))
        this.contentWindow.document.head.append(cssAppend)
      }
    })
  }

  initTabs (element) {
    // Event listener for the tabs
    this.createTabs(element)
    $(`.${this.selectors.tabList}`).addEventListener('Styleguide:tabsInit', function (e) {
      let hashTab = window.location.hash
      if (hashTab !== '') {
        this.querySelector('[name=' + hashTab.replace('#', '') + ']').click()
      } else {
        this.querySelector('a').click()
      }
    })
    // set an event for tabs init
    let tabCreationEvent = document.createEvent('Event')
    tabCreationEvent.initEvent('Styleguide:tabsInit', false, true)
    $(`.${this.selectors.tabList}`).dispatchEvent(tabCreationEvent)
  }

  createTabs (element) {
    let dataSources = JSON.parse(element.getAttribute('data-source'))
    let baseURL = element.getAttribute('href').split('.html')
    let tabList = $(`.${this.selectors.tabList}`)
    let self = this

    // unbind old tabs
    Array.prototype.slice.call(tabList.querySelectorAll('li')).forEach((element) => {
      element._.unbind('click')
    })
    // remove old tabs
    while (tabList.lastChild) {
      tabList.removeChild(tabList.lastChild)
    }
    // init to first datatype
    self.dataType = dataSources[Object.keys(dataSources)[0]]

    // loop through json object to generate tabs
    for (let key in dataSources) {
      if (dataSources.hasOwnProperty(key)) {
        let iframeSrc = `${baseURL[0]}.${key}`
        let index = Object.keys(dataSources).indexOf(key)
        let tabItem = $.create('li',
          {
            className: `${self.selectors.tabList}-item`,
            delegate: {
              click: {
                a: function () {
                  let tabsList = this.parentNode
                  self.dataType = this.querySelector('a').name
                  // remove active indicator from all tabs
                  Array.prototype.slice.call(tabsList.querySelectorAll('li')).forEach((element) => {
                    element.removeAttribute('data-active')
                  })
                  // allows iframe to retain style property if example
                  $(`.${self.selectors.tabList}`).setAttribute('data-viewportsize', self.dataType)
                  self.iframeStyle = $(`.${self.selectors.iframeContent}`).getAttribute('style')
                  if (self.dataType !== 'example') {
                    $(`.${self.selectors.iframeContent}`).style = ''
                  } else {
                    $(`.${self.selectors.iframeContent}`).style = self.iframeStyle
                  }
                  // set active indicator to active tabs
                  this.setAttribute('data-active', '')
                  window.history.replaceState({}, this.getAttribute('title'), `#${self.dataType}`)
                }
              }
            },
            contents: {
              tag: 'a', href: iframeSrc, textContent: dataSources[key], target: 'StyleguideExample', name: dataSources[key].toLowerCase(), className: `${self.selectors.tabList}-link`
            }
          })
        if (index === 0) {
          $(`.${self.selectors.tabList}`).setAttribute('data-viewportsize', dataSources[key].toLowerCase())
          tabItem.setAttribute('data-active', '')
        }
        $(`.${this.selectors.tabList}`)._.contents(tabItem)
      }
    }
  }

}
