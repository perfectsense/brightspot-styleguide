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

  get devices () {
    return this.settings.devices
  }

  constructor (ctx, options = {}) {
    this.$ctx = $(ctx)
    this.settings = Object.assign({}, {
      selectors: {
        controls: 'StyleguideViewport',
        example: 'StyleguideExample',
        iframe: 'StyleguideExample-frame'
      }
    }, options)
    this.$widthInput = this.$ctx.querySelector(`.${this.selectors.controls}-width`)
    this.$heightInput = this.$ctx.querySelector(`.${this.selectors.controls}-height`)
    this.widthInputMin = this.$widthInput.getAttribute('min')
    this.heightInputMin = this.$heightInput.getAttribute('min')
    this.$viewportResizer = this.$ctx.nextElementSibling.querySelector(`.${this.selectors.iframe}`)
  }

  init () {
    let self = this
    // bind actions for viewport controls
    // bind width input field
    // init the viewport on load

    self.updateViewportContainer()
    // also update the input field with width
    self.updateInputs()

    this.$widthInput.addEventListener('focusout', function () {
      if (this.value >= self.widthInputMin || this.value === '') {
        self.updateViewport({width: this.value})
      }
    })
    // bind height input field
    this.$heightInput.addEventListener('focusout', function () {
      if (this.value >= self.heightInputMin || this.value === '') {
        self.updateViewport({height: this.value})
      }
    })
    // bind function to the reset button
    this.$ctx.querySelector(`.${this.selectors.controls}-reset`).addEventListener('click', () => {
      this.updateViewport({width: '', height: ''})
    })
    // bind function based on preset viewports
    this.$ctx.querySelectorAll('[data-viewportsize]').forEach(function (element) {
      element.addEventListener('click', function () {
        self.updateViewport(JSON.parse(this.getAttribute('data-viewportsize')))
      })
    })
    // bind to Styleguide:updateViewport eventlistener
    this.$ctx.addEventListener('Styleguide:updateViewport', function (event) {
      self.updateViewportContainer()
      // also update the input field with width
      self.updateInputs()
    })
    // listens for tab change event and removes or adds width of viewport
    this.$ctx.nextElementSibling.querySelector(`.${this.selectors.iframe}`).addEventListener('Styleguide:tabChange', function (event) {
      if (window.location.hash === '#example') {
        this.style.width = `${self.viewportWidth}px`
        this.style.height = `${self.viewportHeight}px`
      } else {
        this.removeAttribute('style')
      }
    })

    // create handles for resizer
    this.$ctx.nextElementSibling._.contents({contents: [
      {tag: 'div', className: `${this.selectors.example}-handle-ew`},
      {tag: 'div', className: `${this.selectors.example}-handle-nwse`},
      {tag: 'div', className: `${this.selectors.example}-handle-ns`}
    ]})

    this.$ctx.nextElementSibling.addEventListener('mousedown', function (event) {
      self.initDrag(event)
      this.setAttribute('data-resizable', '')
    }, false)
  }

  initDrag (mouseDownEvent) {
    this.startX = mouseDownEvent.clientX
    this.startY = mouseDownEvent.clientY

    let computedStyle = document.defaultView.getComputedStyle(this.$viewportResizer)
    this.startWidth = parseInt(computedStyle.width, 10)
    this.startHeight = parseInt(computedStyle.height, 10)

    let doc = document.documentElement
    doc.addEventListener('mousemove', (event) => {
      this.dragContainer(event, mouseDownEvent)
      this.updateInputs()
    }, false)

    doc.addEventListener('mouseup', () => {
      this.stopDrag()
    }, false)
  }

  dragContainer (event, handleEvent) {
    const newHeight = (this.startHeight + event.clientY - this.startY)
    const newWidth = (this.startWidth + event.clientX - this.startX)

    if (handleEvent.target.classList.contains(`${this.selectors.example}-handle-ns`)) {
      this.viewportHeight = (newHeight > this.heightInputMin) ? newHeight : this.heightInputMin
    }

    if (handleEvent.target.classList.contains(`${this.selectors.example}-handle-ew`)) {
      this.viewportWidth = (newWidth > this.widthInputMin) ? newWidth : this.widthInputMin
    }

    if (handleEvent.target.classList.contains(`${this.selectors.example}-handle-nwse`)) {
      this.viewportHeight = (newHeight > this.heightInputMin) ? newHeight : this.heightInputMin
      this.viewportWidth = (newWidth > this.widthInputMin) ? newWidth : this.widthInputMin
    }

    this.$viewportResizer.style.width = `${this.viewportWidth}px`
    this.$viewportResizer.style.height = `${this.viewportHeight}px`
  }

  stopDrag ($container) {
    this.$viewportResizer.parentNode.removeAttribute('data-resizable')
    document.documentElement._.unbind('mousemove')
    document.documentElement._.unbind('mouseup')
    this.updateViewport({width: this.viewportWidth, height: this.viewportHeight})
  }

  updateInputs () {
    this.$widthInput.value = ''
    if (this.viewportWidth) {
      this.$widthInput.value = this.viewportWidth
    }

    this.$heightInput.value = ''
    if (this.viewportHeight) {
      this.$heightInput.value = this.viewportHeight
    }
  }

  updateViewportSize (search) {
    // set the width of the iframe
    this.viewportWidth = search['width']
    this.$viewportResizer.style.width = `${this.viewportWidth ? this.viewportWidth + 'px' : ''}`

    // set the height of the iframe
    this.viewportHeight = search['height']
    this.$viewportResizer.style.height = `${this.viewportHeight ? this.viewportHeight + 'px' : ''}`
  }

  updateViewportContainer () {
    let search = Util.locationSearchToObject(window.location.search)
    // set the active state of the viewport buttons based on what width and height are set in the search url
    this.$ctx.querySelectorAll('button').forEach(function (element) {
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

    this.updateViewportSize(search)

    if (this.viewportHeight || this.viewportWidth) {
      this.$viewportResizer.parentNode.setAttribute('data-viewportset', '')
    } else {
      this.$viewportResizer.parentNode.removeAttribute('data-viewportset')
    }
  }

  updateViewport (deviceViewport) {
    let baseUrl = Util.updateSearchURL(window.location, deviceViewport)
    window.history.pushState({}, 'Update DeviceViewport', baseUrl)
    // set an event for viewport change
    let updateViewportEvent = document.createEvent('Event')
    updateViewportEvent.initEvent('Styleguide:updateViewport', false, true)
    this.$ctx.dispatchEvent(updateViewportEvent)
  }
}
