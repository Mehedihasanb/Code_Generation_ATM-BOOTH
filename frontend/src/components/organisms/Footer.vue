<script setup>
import { ref } from 'vue'
import '../../assets/footer.css'

const activeModal = ref(null)

const isSending = ref(false)

const openModal = (type) => {
  activeModal.value = type
}

const closeModal = () => {
  activeModal.value = null
  isSending.value = false
}

const submitSupport = () => {
  isSending.value = true
  
  setTimeout(() => {
    isSending.value = false
    activeModal.value = 'success'
    
    setTimeout(() => {
      if (activeModal.value === 'success') closeModal()
    }, 3000)
  }, 1500)
}
</script>

<template>
  <footer class="footer">
    <div class="footer-container">
      <div class="footer-text">
        &copy; {{ new Date().getFullYear() }} RhinoBank. Not all rights reserved.
      </div>
      
      <div class="footer-links">
        <button @click="openModal('privacy')">Privacy Notice</button>
        <button @click="openModal('cookie')">Cookie Policy</button>
        <button @click="openModal('contact')">Contact Support</button>
      </div>
    </div>
  </footer>

  <div v-if="activeModal" class="modal-overlay">
    
    <div class="modal">
      
      <button @click="closeModal" class="modal-close-btn">
        <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12"></path></svg>
      </button>

      <div v-if="activeModal === 'privacy'" class="modal-content">
        <h3 class="modal-title">Privacy Notice</h3>
        <div class="modal-text">
          <p><strong>1. Information We Collect:</strong> We collect your name, email address, and vehicle listing details when you use our platform.</p>
          <p><strong>2. How We Use It:</strong> Your information is used solely to confirm your identity and keep all users safe. We do not sell your personal data to third parties.</p>
          <p><strong>3. Data Security:</strong> Passwords are securely hashed.  </p>
          <p><strong>4. Your Rights:</strong> You have the right to request the deletion of your account and all associated data at any time via your Settings page.</p>
        </div>
        <button @click="closeModal" class="btn-primary blue" style="margin-top: 1.5rem;">I Understand</button>
      </div>

      <div v-else-if="activeModal === 'cookie'" class="modal-content">
        <h3 class="modal-title">Cookie Policy</h3>
        <div class="modal-text">
          <p>This application uses cookies strictly for essential functionality.</p>
          <ul>
            <li><strong>Authentication:</strong> We use secure HTTP-only cookies to keep you logged in securely.</li>
            <li><strong>No Tracking:</strong> We do not use third-party tracking, advertising, or marketing cookies.</li>
          </ul>
          <p class="small-text">By continuing to use this site, you consent to our use of these essential cookies.</p>
        </div>
        <button @click="closeModal" class="btn-primary blue" style="margin-top: 1.5rem;">Accept Essential Cookies</button>
      </div>

      <div v-else-if="activeModal === 'contact'" class="modal-content">
        <h3 class="modal-title">Contact Support</h3>
        <p class="modal-description">Need help? Send us a message and we'll get back to you.</p>
        
        <form @submit.prevent="submitSupport" class="modal-form">
          <div class="form-group">
            <label class="form-label">Issue Category</label>
            <select class="form-select">
              <option>Account Help</option>
              <option>Listing Issue</option>
              <option>Report a suspicious activity</option>
              <option>Other</option>
            </select>
          </div>
          <div class="form-group">
            <label class="form-label">Message</label>
            <textarea required class="form-textarea" placeholder="Describe your issue..."></textarea>
          </div>
          <button type="submit" :disabled="isSending" class="btn-primary dark">
            <span v-if="isSending">
              <svg class="spinner" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path></svg>
              Sending...
            </span>
            <span v-else>Send Message</span>
          </button>
        </form>
      </div>

      <div v-else-if="activeModal === 'success'" class="modal-content modal-success">
        <div class="success-icon">
          <svg fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="3" d="M5 13l4 4L19 7"></path></svg>
        </div>
        <h3 class="success-title">Message Sent!</h3>
        <p class="success-text">Our support team will review your request and get back to you shortly.</p>
      </div>

    </div>
  </div>
</template>

