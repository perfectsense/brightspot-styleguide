/* eslint-disable no-unused-vars */
import Bliss from 'bliss'
import Util from './util.js'
/* global $, $$ */

export class DeviceViewport {
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

  get devices () {
    return this.settings.devices
  }

  constructor (ctx, options = {}) {
    this.ctx = ctx
    this.settings = Object.assign({}, {
      selectors: {
        controls: 'StyleguideViewport',
        viewport: 'StyleguideViewport-resize'
      }
    }, options)
  }

  init () {
    console.log(this.ctx)
    let self = this
    // bind width input field
    $(`.${this.selectors.controls}-width`).addEventListener('focusout', function () {
      self.updateDeviceViewport({width: this.value})
    })
    // bind height input field
    $(`.${this.selectors.controls}-height`).addEventListener('focusout', function () {
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

    let $resizeContainer = $(`.${this.selectors.viewport}`)
    $resizeContainer._.contents($.create('div', {
      className: 'StyleguideViewport-resizer',
      events: {
        mousedown: function (event) {
          self.initDrag(event)
        }
      }
    }))

    // init the viewport
    self.updateViewport()
  }

  initDrag (event) {
    this.startX = event.clientX
    this.startY = event.clientY

    let computedStyle = document.defaultView.getComputedStyle($(`.${this.selectors.viewport}`))
    this.startWidth = parseInt(computedStyle.width, 10)
    this.startHeight = parseInt(computedStyle.height, 10)

    let doc = document.documentElement
    doc.addEventListener('mousemove', (event) => {
      this.dragContainer(event)
    }, false)

    doc.addEventListener('mouseup', () => {
      this.stopDrag()
    }, false)

    doc.addEventListener('mousedown', () => {
      $(`.${this.selectors.viewport}`).setAttribute('data-resizable', '')
    }, false)
  }

  dragContainer (event) {
    let $resizeContainer = $(`.${this.selectors.viewport}`)
    this.viewportWidth = (this.startWidth + event.clientX - this.startX)
    this.viewportHeight = (this.startHeight + event.clientY - this.startY)
    $resizeContainer.style.width = `${this.viewportWidth}px`
    $resizeContainer.style.height = `${this.viewportHeight}px`
  }

  stopDrag () {
    let $resizeContainer = $(`.${this.selectors.viewport}`)
    $resizeContainer.removeAttribute('data-resizable')
    document.documentElement._.unbind('mousemove')
    document.documentElement._.unbind('mouseup')
    this.updateInputs()
  }

  updateInputs () {
    let $viewportWidthInput = $(`.${this.selectors.controls}-width`)

    if (this.viewportWidth !== '') {
      $viewportWidthInput.value = this.viewportWidth
    } else {
      $viewportWidthInput.value = ''
    }
    let $viewportHeightInput = $(`.${this.selectors.controls}-height`)
    if (this.viewportHeight !== '') {
      $viewportHeightInput.value = this.viewportHeight
    } else {
      $viewportHeightInput.value = ''
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
    // also update the input field with width
    let $viewport = $(`.${this.selectors.viewport}`)
    let $inputWidth = $(`.${this.selectors.controls}-width`)
    if (search['width']) {
      this.viewportWidth = search['width']
      $viewport.style.width = `${this.viewportWidth}px`
    } else {
      this.viewportWidth = ''
      $viewport.style.width = this.viewportWidth
    }

    // set the height of the iframe
    // also update the input field with height
    let $inputHeight = $(`.${this.selectors.controls}-height`)
    if (search['height']) {
      this.viewportHeight = search['height']
      $viewport.style.height = `${this.viewportHeight}px`
    } else {
      this.viewportHeight = ''
      $viewport.style.height = this.viewportHeight
    }

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
