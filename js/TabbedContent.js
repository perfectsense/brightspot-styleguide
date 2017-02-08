/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Prism from 'prism'
import PrismJson from 'prism-json'
import PrismMarkdown from 'prism-markdown'

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

  constructor (ctx, options = {}) {
    this.ctx = ctx
    this.settings = Object.assign({}, {
      selectors: {
        tabList: '.StyleguideTabs',
        languageClass: 'language-',
        iframeContent: 'StyleguideExample'
      }
    }, options)
  }

  init() {
    let self = this
    $(`.${this.selectors.iframeContent}`).addEventListener('load', function (event) {
      var prismElement = this.contentWindow.document.querySelector('pre')
      console.log('iframe loaded', prismElement)
      if (prismElement !== null) {
        console.log(self.selectors.languageClass + self.dataType)
        prismElement.className = self.selectors.languageClass + self.dataType
        Prism.highlightElement(prismElement)
        let cssAppend = $.clone($('link[href="/_styleguide/index.css"]'))
        this.contentWindow.document.head.append(cssAppend)
      }
    })
  }

  createTabs (element) {
    let dataSources = JSON.parse(element.getAttribute('data-source'))
    let baseURL = element.getAttribute('href').split('.html')
    let tabList = $(this.selectors.tabList)
    let self = this
    // unbind old tabs
    tabList.querySelectorAll('li').forEach((element) => {
      element._.unbind('click')
    })
    // remove old tabs
    while (tabList.lastChild) {
      tabList.removeChild(tabList.lastChild)
    }
    // loop through json object to generate tabs
    for (var key in dataSources) {
      if (dataSources.hasOwnProperty(key)) {
        let iframeSrc = baseURL[0] + '.' + key

        let tabItem = $.create('li',
          {
            delegate: {
              click: {
                a: function () {
                  let tabsList = this.parentNode
                  // remove active indicator from all tabs
                  tabsList.querySelectorAll('li').forEach((element) => {
                    element.removeAttribute('data-active')
                  })
                  // set active indicator to active tabs
                  this.setAttribute('data-active', '')
                  self.dataType = this.querySelector('a').name
                  console.log(self.dataType)
                }
              }
            },
            contents: {
              tag: 'a', href: iframeSrc, textContent: dataSources[key], target: 'StyleguideExample', name: dataSources[key].toLowerCase()
            }
          })
        $(this.selectors.tabList)._.contents(tabItem)
      }
    }
  }

}
