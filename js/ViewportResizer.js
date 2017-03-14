/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Util from './util.js'
/* global $, $$ */

export class ViewportResizer {
  get selectors () {
    return this.settings.selectors
  }

  get startX () {
    return this._startX
  }

  set startX (X) {
    this._startX = X
  }

  get startY () {
    return this._startY
  }

  set startY (Y) {
    this._startY = Y
  }
  get startWidth () {
    return this._startWidth
  }

  set startWidth (width) {
    this._startWidth = width
  }

  get startHeight () {
    return this._startHeight
  }

  set startHeight (height) {
    this._startHeight = height
  }

  get viewportWidth () {
    return this._viewportWidth
  }

  set viewportWidth (height) {
    this._viewportWidth = height
  }

  get viewportHeight () {
    return this._viewportHeight
  }

  set viewportHeight (height) {
    this._viewportHeight = height
  }

  get $resizeContainer () {
    return $(`.${this.selectors.viewport}`)
  }

  get $viewportWidthInput () {
    return $(`.${this.selectors.controls}-width`)
  }

  get $viewportHeightInput () {
    return $(`.${this.selectors.controls}-height`)
  }

  get devices () {
    return this.settings.devices
  }

  constructor (ctx, options = {}) {
    this.ctx = ctx
    this.settings = Object.assign({}, {
      selectors: {
        controls: 'StyleguideViewport',
        viewport: 'StyleguideViewport-resize',
        iframe: 'StyleguideExample'
      }
    }, options)
  }

  init () {
    let self = this
    // bind actions for viewport controls
    // bind width input field
    // init the viewport on load
    self.updateViewport()

    this.$viewportWidthInput.addEventListener('focusout', function () {
      self.updateDeviceViewport({width: this.value})
    })
    // bind height input field
    this.$viewportHeightInput.addEventListener('focusout', function () {
      self.updateDeviceViewport({height: this.value})
    })
    // bind function to the reset button
    $(`.${this.selectors.controls}-reset`).addEventListener('click', () => {
      this.updateDeviceViewport({width: '', height: ''})
    })
    // bind function based on preset viewports
    $$('[data-viewportsize]').forEach(function (element) {
      element.addEventListener('click', function () {
        self.updateDeviceViewport(JSON.parse(this.getAttribute('data-viewportsize')))
      })
    })
    // bind to Styleguide:updateViewport eventlistener
    $(`.${this.selectors.controls}-controls`).addEventListener('Styleguide:updateViewport', function (event) {
      self.updateViewport()
    })
    // listens for tab change event and removes or adds width of viewport
    $(`.${this.selectors.iframe}`).addEventListener('Styleguide:tabChange', (event) => {
      if (window.location.hash === '#example') {
        this.$resizeContainer.style.width = `${this.viewportWidth}px`
        this.$resizeContainer.style.height = `${this.viewportHeight}px`
      } else {
        this.$resizeContainer.removeAttribute('style')
      }
    })
    // bind viewport frame actions
    this.$resizeContainer._.contents($.create('div', {
      className: `${this.selectors.viewport}-handle`
    }))

    this.$resizeContainer.addEventListener('mousedown', (event) => {
      self.initDrag(event)
      self.$resizeContainer.setAttribute('data-resizable', '')
    }, false)
  }

  initDrag (event) {
    this.startX = event.clientX
    this.startY = event.clientY

    let computedStyle = document.defaultView.getComputedStyle(this.$resizeContainer)
    this.startWidth = parseInt(computedStyle.width, 10)
    this.startHeight = parseInt(computedStyle.height, 10)

    let doc = document.documentElement
    doc.addEventListener('mousemove', (event) => {
      this.dragContainer(event, this.$resizeContainer)
    }, false)

    doc.addEventListener('mouseup', () => {
      this.stopDrag(this.$resizeContainer)
    }, false)
  }

  dragContainer (event, $container) {
    this.viewportWidth = (this.startWidth + event.clientX - this.startX)
    this.viewportHeight = (this.startHeight + event.clientY - this.startY)
    $container.style.width = `${this.viewportWidth}px`
    $container.style.height = `${this.viewportHeight}px`
  }

  stopDrag ($container) {
    $container.removeAttribute('data-resizable')
    document.documentElement._.unbind('mousemove')
    document.documentElement._.unbind('mouseup')
    this.updateDeviceViewport({width: this.viewportWidth, height: this.viewportHeight})
  }

  updateInputs () {
    if (this.viewportWidth !== '') {
      this.$viewportWidthInput.value = this.viewportWidth
    } else {
      this.$viewportWidthInput.value = ''
    }

    if (this.viewportHeight !== '') {
      this.$viewportHeightInput.value = this.viewportHeight
    } else {
      this.$viewportHeightInput.value = ''
    }
  }
  updateViewport () {
    let search = Util.locationSearchToObject(window.location.search)
    // set the active state of the viewport buttons based on what width and height are set in the search url
    $$(`.${this.selectors.controls}-controls button`).forEach(function (element) {
      let viewportsize = element.getAttribute('data-viewportsize')
      element.removeAttribute('data-active')

      if (viewportsize === null) {
        if (search['width'] === undefined && search['height'] === undefined) {
          element.setAttribute('data-active', '')
        }
        return
      }

      let viewportDimensions = JSON.parse(viewportsize)
      if (viewportDimensions.width === search['width'] && viewportDimensions.height === search['height']) {
        element.setAttribute('data-active', '')
      }
    })
    // set the width of the iframe
    if (search['width']) {
      this.viewportWidth = search['width']
      this.$resizeContainer.style.width = `${this.viewportWidth}px`
    } else {
      this.viewportWidth = ''
      this.$resizeContainer.style.width = this.viewportWidth
    }

    // set the height of the iframe
    if (search['height']) {
      this.viewportHeight = search['height']
      this.$resizeContainer.style.height = `${this.viewportHeight}px`
    } else {
      this.viewportHeight = ''
      this.$resizeContainer.style.height = this.viewportHeight
    }
    // also update the input field with width
    this.updateInputs()
  }

  updateDeviceViewport (deviceViewport) {
    let baseUrl = this.updateSearchURL(window.location, deviceViewport)
    window.history.pushState({}, 'Update DeviceViewport', baseUrl)
    // set an event for viewport change
    let updateViewportEvent = document.createEvent('Event')
    updateViewportEvent.initEvent('Styleguide:updateViewport', false, true)
    $(`.${this.selectors.controls}-controls`).dispatchEvent(updateViewportEvent)
  }

  updateSearchURL (locationObj, paramObj) {
    let url = locationObj.search
    let urlSearch = url.split('?')
    if (urlSearch.length >= 2) {
      let urlParams = urlSearch[1].split(/[&;]/g)
      let hashParam = ''
      let hash = locationObj.hash
      for (let param in paramObj) {
        let prefix = encodeURIComponent(param) + '='
        for (let i = 0; i < urlParams.length; i++) {
          if (urlParams[i].lastIndexOf(prefix, 0) !== -1) {
            urlParams.splice(i, 1)
          }
        }

        if (paramObj[param] !== '') {
          urlParams.push(`${param}=${paramObj[param]}`)
        }
      }
      urlSearch = `?${urlParams.join('&')}${locationObj.hash}`
    }
    return urlSearch
  }
}
